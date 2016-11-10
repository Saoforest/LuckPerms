/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.commands.log.subcommands;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Patterns;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;

public class LogRecent extends SubCommand<Log> {
    public LogRecent() {
        super("recent", "View recent actions", Permission.LOG_RECENT, Predicates.notInRange(0, 2),
                Arg.list(
                        Arg.create("user", false, "the name/uuid of the user to filter by"),
                        Arg.create("page", false, "the page number to view")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Log log, List<String> args, String label) throws CommandException {
        if (args.size() == 0) {
            // No page or user
            return showLog(log.getRecentMaxPages(), null, sender, log);
        }

        if (args.size() == 1) {
            // Page or user
            try {
                int p = Integer.parseInt(args.get(0));
                // page
                return showLog(p, null, sender, log);
            } catch (NumberFormatException ignored) {}
        }

        // User and possibly page
        final String s = args.get(0);
        UUID u = null;

        u = Util.parseUuid(s);
        if (u == null) {
            if (s.length() <= 16) {
                if (Patterns.NON_USERNAME.matcher(s).find()) {
                    Message.USER_INVALID_ENTRY.send(sender, s);
                    return CommandResult.INVALID_ARGS;
                }

                UUID uuid = plugin.getDatastore().getUUID(s).getUnchecked();

                if (uuid == null) {
                    Message.USER_NOT_FOUND.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() != 2) {
                    // Just user
                    return showLog(log.getRecentMaxPages(uuid), uuid, sender, log);
                }

                try {
                    int p = Integer.parseInt(args.get(1));
                    // User and page
                    return showLog(p, uuid, sender, log);
                } catch (NumberFormatException e) {
                    // Invalid page
                    return showLog(-1, null, null, null);
                }
            }

            Message.USER_INVALID_ENTRY.send(sender, s);
            return CommandResult.INVALID_ARGS;
        }

        if (args.size() != 2) {
            // Just user
            return showLog(log.getRecentMaxPages(u), u, sender, log);
        } else {
            try {
                int p = Integer.parseInt(args.get(1));
                // User and page
                return showLog(p, u, sender, log);
            } catch (NumberFormatException e) {
                // Invalid page
                return showLog(-1, null, null, null);
            }
        }
    }

    private static CommandResult showLog(int page, UUID filter, Sender sender, Log log) {
        int maxPage = (filter != null) ? log.getRecentMaxPages(filter) : log.getRecentMaxPages();
        if (maxPage == 0) {
            Message.LOG_NO_ENTRIES.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (page < 1 || page > maxPage) {
            Message.LOG_INVALID_PAGE_RANGE.send(sender, maxPage);
            return CommandResult.INVALID_ARGS;
        }

        SortedMap<Integer, LogEntry> entries = (filter != null) ? log.getRecent(page, filter) : log.getRecent(page);
        if (filter != null) {
            String name = entries.values().stream().findAny().get().getActorName();
            Message.LOG_RECENT_BY_HEADER.send(sender, name, page, maxPage);
        } else {
            Message.LOG_RECENT_HEADER.send(sender, page, maxPage);
        }

        for (Map.Entry<Integer, LogEntry> e : entries.entrySet()) {
            Message.LOG_ENTRY.send(sender, e.getKey(), DateUtil.formatDateDiff(e.getValue().getTimestamp()), e.getValue().getFormatted());
        }
        return CommandResult.SUCCESS;
    }
}
