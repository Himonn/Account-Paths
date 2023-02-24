package com.accountpaths;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class AccountPathsTile {

    @Getter
    @Setter
    private WorldPoint start;
    @Getter
    @Setter
    private String startLabel;
    @Getter
    @Setter
    private WorldPoint end;
    @Getter
    @Setter
    private String endLabel;
    @Getter
    @Setter
    private Pathfinder pathfinder;
}
