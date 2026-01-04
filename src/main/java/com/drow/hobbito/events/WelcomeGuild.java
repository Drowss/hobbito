package com.drow.hobbito.events;

import lombok.SneakyThrows;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class WelcomeGuild extends ListenerAdapter {
    @SneakyThrows
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Thread.sleep(1000);
        event.getGuild()
                .getDefaultChannel()
                .asTextChannel()
                .sendMessage("🎉 Bienvenido " + event.getUser().getAsMention() + " al servidor!")
                .queue();

    }
}
