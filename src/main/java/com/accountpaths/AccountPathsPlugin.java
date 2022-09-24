package com.accountpaths;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    public boolean dev = true;
    String folderPath = "C:\\Users\\micha\\OneDrive\\Desktop\\Radish Remake Andy Plugin\\Repos\\Account-Paths\\src\\main\\resources\\com\\accountpaths\\";

    public Collection<AccountPathsTile> tileCollection = new ArrayList<>();
    public HashMap<Integer, AccountPathsTile> tileMap = new HashMap<>();
    public List<String> resourceFileNames = new ArrayList<>();
    public int index = 0;
    public String title = "";
    public String description = "";
    public String nextTitle = "";
    public String nextDescription = "";

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
        getJsonResources();
        index = config.index();
        loadJson(index);
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
            configManager.setConfiguration("accountpaths", "index", index);
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
            configManager.setConfiguration("accountpaths", "index", index);
        }
    };

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
                JSONObject tile = tileArray.getJSONObject(i);

                String label = tile.getString("label");
                int x = tile.getInt("x");
                int y = tile.getInt("y");
                int z = tile.getInt("z");
                int region = tile.getInt("region");
                JSONArray positions = tile.getJSONArray("positions");

                WorldPoint wp = WorldPoint.fromRegion(region, x, y, z);
                AccountPathsTile accountPathsTile = new AccountPathsTile();
                accountPathsTile.setLabel(label);
                accountPathsTile.setX(x);
                accountPathsTile.setY(y);
                accountPathsTile.setZ(z);
                accountPathsTile.setRegion(region);
                accountPathsTile.setWorldPoint(wp);
                accountPathsTile.setPositions(positions);

                tileCollection.add(accountPathsTile);
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        for (AccountPathsTile tile : tileCollection)
        {
            for (Integer position : tile.getPositionList())
            {
                tileMap.put(position, tile);
            }
        }
    }

    public JSONObject parseJSONFile(String filename) throws JSONException, IOException {
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        return new JSONObject(content);
    }

    public void getJsonResources()
    {
        if (dev)
        {
            File folder = new File(folderPath + config.path().getPath());

            if (folder == null)
            {
                return;
            }

            File[] listFolder = folder.listFiles();

            if (listFolder == null)
            {
                return;
            }

            List<String> list = new ArrayList<>();

            for (int i = 0; i < listFolder.length; i++)
            {
                if (listFolder[i].isFile())
                {
                    list.add(listFolder[i].getName());
                }
            }

            resourceFileNames = list;
        }

        try {
            resourceFileNames = getResourceFiles("com/accountpaths/" + config.path().getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> getResourceFiles(String path) throws IOException
    {
        List<String> filenames = new ArrayList<>();

        try (
                InputStream in = getResourceAsStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }

        return filenames;
    }

    private URL getResourceURL(String path)
    {
        return getContextClassLoader().getResource(path);
    }

    private File getFileFromURL(URL url) throws URISyntaxException
    {
        return Paths.get(url.toURI()).toFile();
    }

    private InputStream getResourceAsStream(String resource)
    {
        final InputStream in
                = getContextClassLoader().getResourceAsStream(resource);

        return in == null ? getClass().getResourceAsStream(resource) : in;
    }

    private ClassLoader getContextClassLoader()
    {
        return Thread.currentThread().getContextClassLoader();
    }
}