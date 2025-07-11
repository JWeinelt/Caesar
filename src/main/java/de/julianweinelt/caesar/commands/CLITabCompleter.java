package de.julianweinelt.caesar.commands;

import de.julianweinelt.caesar.util.StringUtil;

import java.util.Arrays;
import java.util.List;

public abstract class CLITabCompleter {
    public abstract List<String> onTabCompletion(String[] args);




    public void complete(List<String> list, String arg, String... completions) {
        list.clear();
        StringUtil.copyPartialMatches(arg, Arrays.asList(completions), list);
    }
    public void complete(List<String> list, String arg, List<String> completions) {
        list.clear();
        StringUtil.copyPartialMatches(arg, completions, list);
    }

    public void completeEmpty(List<String> list) {
        list.clear();
    }
}
