package org.cubeville.commons.commands;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import org.bukkit.potion.PotionEffectType;
import org.cubeville.commons.utils.ColorUtils;

public class CommandParser
{
    List<BaseCommand> commands;

    public CommandParser() {
        commands = new ArrayList<>();
    }

    public void addCommand(BaseCommand command) {
        commands.add(command);
    }

    public List<String> getCompletions(CommandSender sender, String[] argsIn) {
        String full = "";
        for (String arg: argsIn) {
            if(full.length() > 0) full = full + " ";
            full += arg;
        }
        List<String> args = new ArrayList<>(Arrays.asList(smartSplit2(full).toArray(new String[0])));


        List<String> baseCommandCompletions = new ArrayList<>();
        for(BaseCommand cmd : commands) {
            if(cmd.getPermission() == null || sender.hasPermission(cmd.getPermission())) {
                baseCommandCompletions.add(cmd.getFullCommand());
            }
        }
        //NO ARGS
        if(args.isEmpty()) {
            return baseCommandCompletions;
        }

        //ARG INDEX 0
        final String arg0 = args.remove(0);
        if(args.isEmpty()) {
            baseCommandCompletions.removeIf(completion -> !completion.startsWith(arg0.toLowerCase()));
            return baseCommandCompletions;
        }

        //ARG BASE PARAMETERS
        int requiredBase = 0;
        for(BaseCommand cmd : commands) {
            if(cmd.getFullCommand().equalsIgnoreCase(arg0)) {
                if(cmd.getPermission() == null || sender.hasPermission(cmd.getPermission())) {
                    requiredBase = cmd.mandatoryBase;
                }
                break;
            }
        }
        if(requiredBase - (args.size() - 1) > 0) {
            return null;
        }

        //ANY OTHER ARGS
        List<String> parameterCompletions = new ArrayList<>();
        for(BaseCommand cmd : commands) {
            if(cmd.getFullCommand().equalsIgnoreCase(arg0)) {
                if(cmd.getPermission() == null || sender.hasPermission(cmd.getPermission())) {
                    for(String m : cmd.getMandatory().keySet()) {
                        parameterCompletions.add(m.concat(":"));
                    }
                    for(String o : cmd.getOptional().keySet()) {
                        parameterCompletions.add(o.concat(":"));
                    }
                    parameterCompletions.addAll(cmd.getFlags());
                }
                break;
            }
        }

        if(!args.isEmpty()) {
            final String nextArg = args.get(args.size() - 1);
            for(String a : args) {
                parameterCompletions.removeIf(completion -> completion.equalsIgnoreCase(a) || (a.contains(":") && completion.equalsIgnoreCase(a.substring(0, a.indexOf(":") + 1))));
            }
            parameterCompletions.removeIf(completion -> !completion.startsWith(nextArg.toLowerCase()));
            return parameterCompletions;
        }

        return null;
    }

    public boolean execute(CommandSender commandSender, String[] argsIn) {
        try {
            String full = "";
            for (String arg: argsIn) {
                if(full.length() > 0) full = full + " ";
                full += arg;
            }

            String[] args = smartSplit(full).toArray(new String[0]);

            String parameterError = null;
            BaseCommand cmd = null;
            int cmdMatch = -1;
            for(BaseCommand command: commands) {
                if(command.commandMatches(args)) {
                    int match = command.checkCommand(args);
                    if(match > cmdMatch) {
                        parameterError = command.checkParameters(args);
                        if(parameterError == null) {
                            cmd = command;
                            cmdMatch = match;
                        }
                    }
                }
            }

            if(cmdMatch >= 0) { 
                CommandResponse response = cmd.execute(commandSender, args);
                if(response != null && response.isSilent()) return true;
                if(cmd.isSilentConsole() == false || commandSender instanceof Player) {
                    if(response == null) {
                        commandSender.sendMessage(ColorUtils.addColor("&aCommand executed successfully."));
                    }
                    else {
                        if(response.getMessages() == null) {
                            commandSender.sendMessage(ColorUtils.addColor("&cNothing. Nada. Niente."));
                        }
                        else {
                            for (String message: response.getMessages()) {
                                if(!message.equals("")) {
                                    commandSender.sendMessage(ColorUtils.addColor(message));
                                }
                            }
                        }
                    }
                }
                return true;
            } 
            
            if(parameterError != null) {
                commandSender.sendMessage(ColorUtils.addColor("&c" + parameterError));
            }
            
            else {
                commandSender.sendMessage("Unknown command!");
            }
            return false;
        }
        //catch(CommandExecutionException e) {
        //    player.sendMessage(Colorize.addColor(e.getMessage()));
        //    return true;
        //}
        catch(IllegalArgumentException|CommandExecutionException e) {
            String msg = ColorUtils.addColorWithoutHeader(e.getMessage());
            if(ColorUtils.removeColor(msg).equals(msg)) {
                    msg = ColorUtils.addColor("&c" + msg);
            }
            else {
                msg = ColorUtils.addColor(e.getMessage());
            }
            commandSender.sendMessage(msg);
            return true;
        }
    }

    private List<String> smartSplit(String full) {
        // I don't like this piece of code....
        if(full.length() == 0) return new ArrayList<>();
        boolean inQuotes = false;
        List<String> ret = new ArrayList<>();
        ret.add(new String());
        for(int i = 0; i < full.length(); i++) {
            String current = ret.get(ret.size() - 1);
            int lsize = ret.size();
            int size = current.length();
            char c = full.charAt(i);
            char last = (size > 0 ? current.charAt(size - 1) : ' ');

            if(c == '"' && (size == 0 || last == ' ' || last == ':') && inQuotes == false) {
                inQuotes = true;
            }
            else if(c == '"' && inQuotes == true && (i == full.length() - 1 || full.charAt(i + 1) == ' ')) {
                inQuotes = false;
            }
            else if(c == ' ' && inQuotes == false) {
                ret.add(new String());
            }
            else {
                ret.set(lsize - 1, ret.get(lsize - 1) + full.charAt(i));
            }
        }

        if(inQuotes) throw new IllegalArgumentException("Unbalanced quotes!");
        return ret;
    }

    private List<String> smartSplit2(String full) {
        // I don't like this piece of code....
        if(full.length() == 0) return new ArrayList<>();
        boolean inQuotes = false;
        List<String> ret = new ArrayList<>();
        ret.add(new String());
        for(int i = 0; i < full.length(); i++) {
            String current = ret.get(ret.size() - 1);
            int lsize = ret.size();
            int size = current.length();
            char c = full.charAt(i);
            char last = (size > 0 ? current.charAt(size - 1) : ' ');

            if(c == '"' && (size == 0 || last == ' ' || last == ':') && inQuotes == false) {
                inQuotes = true;
            }
            else if(c == '"' && inQuotes == true && (i == full.length() - 1 || full.charAt(i + 1) == ' ')) {
                inQuotes = false;
            }
            else if(c == ' ' && inQuotes == false) {
                ret.add(new String());
            }
            else {
                ret.set(lsize - 1, ret.get(lsize - 1) + full.charAt(i));
            }
        }

        //if(inQuotes) throw new IllegalArgumentException("Unbalanced quotes!");
        return ret;
    }
}
