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
import java.util.HashMap;
import java.util.List;

@Slf4j
@PluginDescriptor(
        name = "<html>[<font color=#fa9b17>H<font color=#ffffff>] Account Paths",
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

    JSONObject jsonObject;
    JSONArray jsonArray;

    public HashMap<WorldPoint, String> currentTiles = new HashMap<>();

    List<String> resourceFileNames = new ArrayList<>();

    public int index = 0;
    public String title = "";
    public String description = "";

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
        JSONArray tileArray = null;

        if (index >= resourceFileNames.size())
        {
            return;
        }

        try {
            jsonFile = getFileFromURL(getResourceURL("com/accountpaths/" + config.path().getPath() + "/" + resourceFileNames.get(index)));
            jsonObject = parseJSONFile(jsonFile.getPath());

            if (jsonObject == null)
            {
                return;
            }

            currentTiles.clear();

            tileArray = jsonObject.getJSONArray("tiles");
            title = jsonObject.getString("title");
            description = jsonObject.getString("description");

            if (tileArray == null)
            {
                return;
            }

            for (int i = 0; i < tileArray.length(); i++) {
                JSONObject tile = tileArray.getJSONObject(i);

                String label = tile.getString("label");
                int x = tile.getInt("x");
                int y = tile.getInt("y");
                int z = tile.getInt("z");
                int region = tile.getInt("region");

                WorldPoint wp = WorldPoint.fromRegion(region, x, y, z);
                currentTiles.put(wp, label);
            }

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    public JSONObject parseJSONFile(String filename) throws JSONException, IOException {
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        return new JSONObject(content);
    }

    public void getJsonResources()
    {
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