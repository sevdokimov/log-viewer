package com.logviewer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class LogViewerMain {

    private static final Set<String> HELP_KEYS = ImmutableSet.of("--help", "/help", "help");

    private static final List<CommandHandler> handlers = Arrays.asList(
            new StartupJettyCommand()
    );

    static {
        assert handlers.size() == handlers.stream().map(CommandHandler::getCommandName).distinct().count();
    }

    public static void main(String[] args) throws Exception {
        JCommander commander = new JCommander();
        for (CommandHandler handler : handlers) {
            commander.addCommand(handler.getCommandName(), handler.getArgsHolder());
        }

        if (args.length == 0 || Stream.of(args).anyMatch(HELP_KEYS::contains)) {
            commander.setProgramName("smLog.sh");
            commander.usage();
            return;
        }

        try {
            commander.parse(args);
        } catch (MissingCommandException e) {
            System.err.printf("Command is not specified, available commands: %s\n", handlers.stream().map(CommandHandler::getCommandName).collect(Collectors.joining(", ")));

            System.exit(1);
        }

        String command = commander.getParsedCommand();

        CommandHandler commandHandler = handlers.stream().filter(handler -> handler.getCommandName().equals(command)).findAny()
                .orElse(null);

        commandHandler.execute();
    }

}
