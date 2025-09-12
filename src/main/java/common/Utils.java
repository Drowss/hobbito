package common;

import net.dv8tion.jda.api.EmbedBuilder;

public class Utils {
    public static EmbedBuilder createEmbed(String imageUrl, String title, String description, String footer) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setImage(imageUrl);
        embed.setTitle(title);
        embed.setDescription(description);
        embed.setFooter(footer);
        return embed;
    }
}
