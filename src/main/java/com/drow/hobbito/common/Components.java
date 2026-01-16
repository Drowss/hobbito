package com.drow.hobbito.common;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.util.List;

public class Components {
    public static final long CAFFEINE_TTL = 100;
    public static final String FOOTER_ICON =
            "https://media.discordapp.net/attachments/1415545619757531189/1415875828989820968/image.png";
    public static final java.util.List<Color> COLORS = List.of(
            Color.CYAN,
            Color.ORANGE,
            Color.MAGENTA,
            Color.GREEN,
            Color.PINK
    );

    private Components() {
    }

    public static EmbedBuilder buildFrank(String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setImage("https://i.imgur.com/lbLR6Pu.png")
                .setTitle("\uD83D\uDE31 Algo ha preocupado a Frank!")
                .setDescription(description)
                .setColor(Color.RED);
        return embed;
    }
}
