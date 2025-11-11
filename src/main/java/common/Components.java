package common;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class Components {
    public static final long CAFFEINE_TTL = 100;

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
