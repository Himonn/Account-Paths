package com.accountpaths;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AccountPathsTile {

    @Getter
    @Setter
    private int x;
    @Getter
    @Setter
    private int y;
    @Getter
    @Setter
    private int z;
    @Getter
    @Setter
    private int region;
    @Getter
    @Setter
    private String label;
    @Getter
    @Setter
    private WorldPoint worldPoint;
    @Getter
    @Setter
    private JSONArray positions;

    public List<Integer> getPositionList()
    {
        List<Integer> list = new ArrayList<>();
        for (Object o : positions.toList())
        {
            if (o instanceof Integer)
            {
                list.add((Integer) o);
            }
        }

        return list;
    }

}
