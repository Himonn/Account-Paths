package com.accountpaths;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

@Slf4j
@PluginDescriptor(
        name = "Radish Remake Andy",
        enabledByDefault = false,
        description = "AccountPaths"
)
public class AccountPathsPlugin extends Plugin
{
    @Inject
    private Client client;
    @Inject
    private AccountPathsConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private KeyManager keyManager;
    @Inject
    private ConfigManager configManager;
    @Inject
    private AccountPathsOverlay overlay;
    @Inject
    private AccountPathsSceneOverlay sceneOverlay;
    @Inject
    private ChatMessageManager chatMessageManager;
    @Inject
    private ChatboxPanelManager chatboxPanelManager;

    // Dev variables
    public boolean dev = true;
    String folderPath = "C:\\Users\\Simon\\IdeaProjects\\Plugin Hub\\Account-Paths\\src\\main\\resources\\com\\accountpaths\\";

    public boolean generating = false;
    public WorldPoint start = null;
    public WorldPoint end = null;
    public String startLabel = null;
    public String endLabel = null;

    // Variables
    public Collection<AccountPathsTile> tileCollection = new ArrayList<>();
    public List<String> resourceFileNames = new ArrayList<>();
    public int index = 0;
    public String title = "";
    public String description = "";
    public String nextTitle = "";
    public String nextDescription = "";

    @Getter
    private Pathfinder pathfinder;
    private PathfinderConfig pathfinderConfig;

    // Hotkeys
    public HotkeyListener nextHotkey = new HotkeyListener(() -> config.next()) {
        @Override
        public void hotkeyPressed()
        {
            if (index >= resourceFileNames.size())
            {
                return;
            }

            index++;
            loadJson(index);
            config.setIndex(String.valueOf(index));
        }
    };

    public HotkeyListener prevHotkey = new HotkeyListener(() -> config.previous()) {
        @Override
        public void hotkeyPressed()
        {
            if (index == 0)
            {
                return;
            }

            index--;
            loadJson(index);
            config.setIndex(String.valueOf(index));
        }
    };

    @Provides
    AccountPathsConfig provideConfig(final ConfigManager configManager)
    {
        return configManager.getConfig(AccountPathsConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        overlayManager.add(sceneOverlay);
        keyManager.registerKeyListener(nextHotkey);
        keyManager.registerKeyListener(prevHotkey);
        index = config.index();

        getJsonResources();
        loadJson(index);

        CollisionMap map = CollisionMap.fromResources();
        Map<WorldPoint, List<Transport>> transports = Transport.fromResources(config);

        pathfinderConfig = new PathfinderConfig(map, transports, client);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        overlayManager.remove(sceneOverlay);
        keyManager.unregisterKeyListener(nextHotkey);
        keyManager.unregisterKeyListener(prevHotkey);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals("accountpaths"))
        {
            return;
        }

        if (event.getKey().equals("path"))
        {
            getJsonResources();
            index = 0;
            loadJson(index);
        }

        if (event.getKey().equals("index"))
        {
            index = config.index();
            loadJson(index);
        }
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted event)
    {
        if (event.getCommand().equals("gen"))
        {
            generating = !generating;
            sendGameMessage(String.format("Generating: %s", generating));
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (client.isKeyPressed(KeyCode.KC_SHIFT) && event.getType() == MenuAction.WALK.getId() && (generating || dev))
        {
            client.createMenuEntry(-1)
                    .setOption(ColorUtil.prependColorTag("Set Path Start", Color.ORANGE))
                    .setType(MenuAction.RUNELITE)
                    .onClick(this::setTile);

            client.createMenuEntry(-2)
                    .setOption(ColorUtil.prependColorTag("Set Path End", Color.ORANGE))
                    .setType(MenuAction.RUNELITE)
                    .onClick(this::setTile);

            if (start != null)
            {
                client.createMenuEntry(-3)
                        .setOption(ColorUtil.prependColorTag("Set Start Label", Color.ORANGE))
                        .setType(MenuAction.RUNELITE)
                        .onClick(this::label);
            }

            if (end != null)
            {
                client.createMenuEntry(-4)
                        .setOption(ColorUtil.prependColorTag("Set End Label", Color.ORANGE))
                        .setType(MenuAction.RUNELITE)
                        .onClick(this::label);
            }

            if (pathfinder != null)
            {
                client.createMenuEntry(-5)
                        .setOption(ColorUtil.prependColorTag("Copy JSON", Color.ORANGE))
                        .setType(MenuAction.RUNELITE)
                        .onClick(this::copyJson);
            }
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState().equals(GameState.LOGGED_IN))
        {
            loadJson(index);
        }
    }

    public void copyJson(MenuEntry menuEntry)
    {
        JSONObject startObject = getObject(start, startLabel);
        JSONObject endObject = getObject(end, endLabel);

        JSONObject copyable = new JSONObject();
        copyable.put("start", startObject);
        copyable.put("end", endObject);

        String copyString = copyable.toString();
        final StringSelection stringSelection = new StringSelection(Text.removeTags(copyString));
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    private JSONObject getObject(WorldPoint end, String endLabel) {
        JSONObject object = new JSONObject();
        object.put("x", end.getRegionX());
        object.put("y", end.getRegionY());
        object.put("z", end.getPlane());
        object.put("label", endLabel);
        object.put("region", end.getRegionID());

        return object;
    }

    private void setTile(MenuEntry menuEntry)
    {
        String option = menuEntry.getOption();

        final Tile tile = client.getSelectedSceneTile();
        if (tile == null)
        {
            return;
        }

        WorldPoint location = tile.getWorldLocation();

        if (option.contains("Start"))
        {
            start = location;
            sendGameMessage("Start Set");
        }

        if (option.contains("End"))
        {
            end = location;
            sendGameMessage("End Set");
        }

        if (end != null && start != null)
        {
            pathfinder = new Pathfinder(start, end, pathfinderConfig);
            sendGameMessage("Path Generated");
        }
    }

    private void label(MenuEntry menuEntry)
    {
        String option = menuEntry.getOption();

        if (option.contains("Start"))
        {
            chatboxPanelManager.openTextInput("Start label")
                    .value(Optional.ofNullable(startLabel).orElse(""))
                    .onDone((input) ->
                    {
                        input = Strings.emptyToNull(input);
                        startLabel = input;
                        sendGameMessage(String.format("Start Label: %s", startLabel));
                    })
                    .build();
        }

        if (option.contains("End"))
        {
            chatboxPanelManager.openTextInput("End label")
                    .value(Optional.ofNullable(endLabel).orElse(""))
                    .onDone((input) ->
                    {
                        input = Strings.emptyToNull(input);
                        endLabel = input;
                        sendGameMessage(String.format("End Label: %s", endLabel));
                    })
                    .build();
        }
    }

    public void sendGameMessage(String message)
    {
        String chatMessage = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append(message)
                .build();

        chatMessageManager
                .queue(QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(chatMessage)
                        .build());
    }

    // Load a specific json from the resourceFileNames List
    // Convert that JSON into AccountPathsTile collection
    // Populate HashMap of tiles, and positions for path rendering
    public void loadJson(int index)
    {
        File jsonFile = null;
        JSONObject jsonObject = null;
        File nextJsonFile = null;
        JSONObject nextJsonObject = null;
        JSONArray tileArray = null;

        nextTitle = "";
        nextDescription = "";

        if (index >= resourceFileNames.size())
        {
            return;
        }

        try {
            if (dev)
            {
                jsonFile = new File(folderPath + "/" + config.path().getPath() + "/" + resourceFileNames.get(index));

                if (index + 1 < resourceFileNames.size())
                {
                    nextJsonFile = new File(folderPath + "/" + config.path().getPath() + "/" + resourceFileNames.get(index + 1));
                }
            } else {
                jsonFile = getFileFromURL(getResourceURL("com/accountpaths/" + config.path().getPath() + "/" + resourceFileNames.get(index)));

                if (index + 1 < resourceFileNames.size())
                {
                    nextJsonFile = getFileFromURL(getResourceURL("com/accountpaths/" + config.path().getPath() + "/" + resourceFileNames.get(index + 1)));
                }
            }

            jsonObject = parseJSONFile(jsonFile.getPath());

            if (nextJsonFile != null && nextJsonFile.exists())
            {
                nextJsonObject = parseJSONFile(nextJsonFile.getPath());

                if (nextJsonObject != null)
                {
                    nextTitle = nextJsonObject.getString("title");
                    nextDescription = nextJsonObject.getString("description");
                }
            }

            if (jsonObject == null)
            {
                return;
            }

            tileCollection.clear();

            tileArray = jsonObject.getJSONArray("tiles");
            title = jsonObject.getString("title");
            description = jsonObject.getString("description");

            if (tileArray == null)
            {
                return;
            }

            for (int i = 0; i < tileArray.length(); i++)
            {
                JSONObject set = tileArray.getJSONObject(i);

                if (!set.has("start") && !set.has("end"))
                {
                    continue;
                }

                JSONObject startObject = set.getJSONObject("start");

                WorldPoint start = WorldPoint.fromRegion(startObject.getInt("region"),
                        startObject.getInt("x"), startObject.getInt("y"), startObject.getInt("z"));

                String startLabel;
                if (startObject.has("label"))
                {
                    startLabel = startObject.getString("label");
                } else {
                    startLabel = "";
                }

                JSONObject endObject = set.getJSONObject("end");

                WorldPoint end = WorldPoint.fromRegion(endObject.getInt("region"),
                        endObject.getInt("x"), endObject.getInt("y"), endObject.getInt("z"));

                String endLabel;
                if (endObject.has("label"))
                {
                    endLabel = endObject.getString("label");
                } else {
                    endLabel = "";
                }

                Pathfinder pathfinder = new Pathfinder(start, end, pathfinderConfig);

                if (pathfinder == null || pathfinder.getPath() == null)
                {
                    continue;
                }

                AccountPathsTile apt = new AccountPathsTile(start, startLabel, end, endLabel, pathfinder);

                tileCollection.add(apt);

            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    // Populates the resourceFileNames list with filenames for all of the JSONs for selected path
    public void getJsonResources()
    {
        // If dev mode, load JSON file names from folder, rather than resources, so we can quickly reload
        if (dev)
        {
            File folder = new File(folderPath + config.path().getPath());
            File[] listFolder = folder.listFiles();

            if (listFolder == null)
            {
                return;
            }

            List<String> list = new ArrayList<>();

            for (File file : listFolder)
            {
                if (file.isFile())
                {
                    list.add(file.getName());
                }
            }

            resourceFileNames = list;
            return;
        }

        // If not dev mode, load json file names from resources
        try {
            resourceFileNames = getResourceFiles("com/accountpaths/" + config.path().getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Reads specified file, returns parsed JSONObject
    public JSONObject parseJSONFile(String filename) throws JSONException, IOException
    {
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        return new JSONObject(content);
    }

    // Returns a list of file names from the specified path in resources
    private List<String> getResourceFiles(String path) throws IOException
    {
        List<String> filenames = new ArrayList<>();

        try (InputStream in = getResourceAsStream(path);
             BufferedReader br = new BufferedReader(new InputStreamReader(in)))
        {
            String resource;

            while ((resource = br.readLine()) != null)
            {
                filenames.add(resource);
            }
        }

        return filenames;
    }

    // Returns URL for specified resource from the current Class Loader
    private URL getResourceURL(String path)
    {
        return getContextClassLoader().getResource(path);
    }

    // Returns the file from the URL Specified
    private File getFileFromURL(URL url) throws URISyntaxException
    {
        return Paths.get(url.toURI()).toFile();
    }

    // Gets all resources from the Class Loader and returns them as stream
    private InputStream getResourceAsStream(String resource)
    {
        final InputStream in
                = getContextClassLoader().getResourceAsStream(resource);

        return in == null ? getClass().getResourceAsStream(resource) : in;
    }

    // Returns current classloader, allows us to always return the correct
    // Class Loader despite different loading methods from intellij / plugin hub jar
    private ClassLoader getContextClassLoader()
    {
        return Thread.currentThread().getContextClassLoader();
    }
}