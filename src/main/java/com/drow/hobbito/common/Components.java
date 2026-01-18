package com.drow.hobbito.common;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    public static String buildAvatarUrl(String username) {
        return "https://www.hobba.tv/habblet/avatarimageByUsername/"
                + username
                + "?size=undefined&direction=2&head_direction=2&headonly=0&action=wav&gesture=sml";
    }

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
