package org.cubeville.commons.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.CommandSender;

public abstract class BaseCommand
{
    String permission;
    private List<String> commands;
    private Set<String> flags;
    private Map<String, CommandParameterType> optional;
    private Map<String, CommandParameterType> mandatory;
    private List<CommandParameterType> base;
    int mandatoryBase;
    boolean silentConsole;
    
    public BaseCommand(String fullCommand) {
        if(fullCommand.length() == 0) {
            commands = new ArrayList<>();
        }
        else {
            commands = Arrays.asList(fullCommand.split(" "));
        }
        flags = new HashSet<>();
        optional = new HashMap<>();
        mandatory = new HashMap<>();
        base = new ArrayList<>();
        silentConsole = false;
    }

    public String getFullCommand() {
        if(commands.size() == 0) return "";
        String ret = commands.get(0);
        for(int i = 1; i < commands.size(); i++) ret += " " + commands.get(i);
        return ret;
    }
    
    public String[] getArgs() {
    	return (String[]) commands.toArray();
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return this.permission;
    }
    
    public void addFlag(String flag) {
        flags.add(flag);
    }

    public Set<String> getFlags() {
        return this.flags;
    }

    public void addBaseParameter(CommandParameterType type) {
        if(base.size() != mandatoryBase) return;
        base.add(type);
        mandatoryBase++;
    }

    public List<CommandParameterType> getBase() {
        return this.base;
    }

    public void addOptionalBaseParameter(CommandParameterType type) {
        base.add(type);
    }

    public Map<String, CommandParameterType> getOptional() {
        return this.optional;
    }
    
    public void addParameter(String name, boolean optional, CommandParameterType type) {
        if(optional) {
            this.optional.put(name, type);
        }
        else {
            mandatory.put(name, type);
        }
    }

    public Map<String, CommandParameterType> getMandatory() {
        return this.mandatory;
    }

    public int checkCommand(String[] args) {
        if(args.length < commands.size()) return 0;
        for(int i = 0; i < commands.size(); i++) {
            if(!args[i].equals(commands.get(i))) {
                System.out.println("Matched: " + i);
                return i;
            }
        }
        return commands.size();
    }

    public boolean commandMatches(String[] args) {
        if(commands.size() == 0) return true;
        if(args.length < commands.size()) return false;
        for(int i = 0; i < commands.size(); i++) {
            if(!args[i].equals(commands.get(i))) {
                return false;
            }
        }
        return true;
    }
    
    public String checkParameters(String[] args) {
        Set<String> flagsChecked = new HashSet<>();
        Set<String> mandatoryParametersChecked = new HashSet<>();
        Set<String> optionalParametersChecked = new HashSet<>();
        int baseParametersSet = 0;

        for(int i = commands.size(); i < args.length; i++) {
            String[] parts = splitArgument(args[i]);
            //String[] parts = args[i].split(":", 2);
            String name = parts[0];
            if(parts.length == 1) {
                if(flags.contains(name)) {
                    if(!flagsChecked.add(name)) {
                        return "Flag '" + name + "' can't be used twice!";
                    }
                }
                else {
                    if(baseParametersSet == base.size()) return "Too many parameters!";
                    if(!base.get(baseParametersSet).isValid(name)) return base.get(baseParametersSet).getInvalidMessage(name);
                    baseParametersSet++;
                }
            }
            else {
                String par = parts[1];
                if(optional.containsKey(name)) {
                    if(!optionalParametersChecked.add(name)) return "Parameter " + name + " can't be used twice!";
                    if(!optional.get(name).isValid(par)) return optional.get(name).getInvalidMessage(name);
                }
                else if(mandatory.containsKey(name)) {
                    if(!mandatoryParametersChecked.add(name)) return "Parameter " + name + " can't be used twice!";
                    if(!mandatory.get(name).isValid(par)) return mandatory.get(name).getInvalidMessage(name);
                }
                else {
                    return "Unknown parameter " + name + "!";
                }
            }
        }
        if(baseParametersSet > base.size() || baseParametersSet < mandatoryBase) {
            return "&cWrong number of parameters!";
        }
        if(mandatoryParametersChecked.size() != mandatory.size()) {
            return "&cMandatory parameter(s) missing!";
        }
        return null;
    }

    public CommandResponse execute(CommandSender commandSender, String[] args) throws CommandExecutionException {
        if(permission != null && !commandSender.hasPermission(permission)) throw new CommandExecutionException("Permission denied.");
        Set<String> flags = new HashSet<>();
        Map<String, Object> parameters = new HashMap<>();
        List<Object> baseParameters = new ArrayList<>();
        for(int i = commands.size(); i < args.length; i++) {
            String[] parts = splitArgument(args[i]);
            String name = parts[0];
            if(parts.length == 1) {
                if(this.flags.contains(name)) {
                    flags.add(name);
                }
                else {
                    baseParameters.add(base.get(baseParameters.size()).getValue(name));
                }
            }
            else {
                if(mandatory.containsKey(name)) {
                    parameters.put(name, mandatory.get(name).getValue(parts[1]));
                }
                else {
                    parameters.put(name, optional.get(name).getValue(parts[1]));
                }
            }
        }
        return execute(commandSender, flags, parameters, baseParameters);
    }

    public String[] splitArgument(String arg) {
        String[] parts = arg.split(":", 2);
        if(parts.length == 1) return parts;
        if(optional.containsKey(parts[0]) || mandatory.containsKey(parts[0])) return parts;
        String[] ret = {arg};
        return ret;
    }
    
    public abstract CommandResponse execute(CommandSender commandSender, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters) throws CommandExecutionException;

    public boolean checkMoreThanOne(Boolean... conditions) {
        int cnt = 0;
        for(Boolean condition: conditions) {
            if(condition) {
                cnt++;
                if(cnt == 2) return true;
            }
        }
        return false;
    }

    public boolean checkExactlyOne(Boolean... conditions) {
        int cnt = 0;
        for(Boolean condition: conditions) {
            if(condition) {
                cnt++;
                if(cnt == 2) return false;
            }
        }
        return cnt == 1;
    }

    public void setSilentConsole() { silentConsole = true; }
    public boolean isSilentConsole() { return silentConsole; }
}
