import cmdmanager.SlashCommands;
import events.RetrieveProfile;
import events.WelcomeGuild;
import interfaces.ICommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        List<ICommand> commandList = List.of(
                new RetrieveProfile()
        );

//        for (ICommand command : commandList) {
//            if (command instanceof Help) {
//                ((Help) command).setCommands(commandList);
//            }
//        }

        JDA jda = JDABuilder.createDefault("MTQxNTU0MzE2ODA0NDI0MTA2OA.GB-cGQ.Wk_A8ZJUETwXQpDPSHPhvrct7ktVSLuwHxyWbE",
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.playing("Snowstorm"))
                .setStatus(OnlineStatus.ONLINE)
                .setAutoReconnect(true)
                .addEventListeners(new SlashCommands(commandList))
                .addEventListeners(new WelcomeGuild())
                .build();
        jda.awaitReady();

        CommandListUpdateAction commands = jda.updateCommands();
        commands.addCommands(
                Commands.slash("profile", "Busca el perfil del Hobba usuario")
                        .addOption(OptionType.STRING, "usuario", "Usuario a buscar", true)
        ).queue();
    }
}
