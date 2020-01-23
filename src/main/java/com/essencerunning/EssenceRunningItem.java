package com.essencerunning;

import java.util.HashMap;
import java.util.Map;

public enum EssenceRunningItem {

    BINDING_NECKLACE("binding necklace", 1),

    EARTH_TALISMAN("earth talisman", 2),

    ENERGY_POTION_1("energy potion(1)", 1),
    ENERGY_POTION_2("energy potion(2)", 1),
    ENERGY_POTION_3("energy potion(3)", 1),
    ENERGY_POTION_4("energy potion(4)", 1),

    RING_OF_DUELING_1("ring of dueling(1)", 1),
    RING_OF_DUELING_2("ring of dueling(2)", 1),
    RING_OF_DUELING_3("ring of dueling(3)", 1),
    RING_OF_DUELING_4("ring of dueling(4)", 1),
    RING_OF_DUELING_5("ring of dueling(5)", 1),
    RING_OF_DUELING_6("ring of dueling(6)", 1),
    RING_OF_DUELING_7("ring of dueling(7)", 1),
    RING_OF_DUELING_8("ring of dueling(8)", 1),

    STAMINA_POTION_1("stamina potion(1)", 1),
    STAMINA_POTION_2("stamina potion(2)", 1),
    STAMINA_POTION_3("stamina potion(3)", 1),
    STAMINA_POTION_4("stamina potion(4)", 1),
    ;

    private final String name;
    private final int quantity;

    private static final Map<String, EssenceRunningItem> map = new HashMap<>(values().length);

    static {
        for (EssenceRunningItem item : values()) {
            map.put(item.getName(), item);
        }
    }

    EssenceRunningItem(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public static EssenceRunningItem of(String name) {
        return map.get(name);
    }
}
