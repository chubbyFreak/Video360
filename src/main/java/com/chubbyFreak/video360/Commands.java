package com.chubbyFreak.video360;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class Commands implements Listener, CommandExecutor {

    public static final String MODE_COMMAND = "mode";
    public static final String CAPTURE_COMMAND = "capture";
    public static final String CANVAS_COMMAND = "canvas";

    public enum Mode {
        RENDER, STREAM
    }

    public enum Canvas {
        SPHERE, FLAT;
    }

    private Mode mode = Mode.STREAM;
    private Canvas canvas = Canvas.SPHERE;
    private boolean toCapture = false;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            switch(cmd.getName()) {
                case MODE_COMMAND:
                    mode = args[0].equals("render") ? Mode.RENDER : Mode.STREAM;
                    sender.sendMessage(ChatColor.GREEN + "Switched to " + args[0] + " mode.");
                    break;
                case CAPTURE_COMMAND:
                    if(mode == Mode.RENDER)
                        toCapture = true;
                    else
                        sender.sendMessage(ChatColor.RED + "Render mode must be enabled before using this command!");
                    break;
                case CANVAS_COMMAND:
                    canvas = args[0].equals("sphere") ? Canvas.SPHERE : Canvas.FLAT;
                    sender.sendMessage(ChatColor.GREEN + "Switched canvas to " + args[0] + ".");
                    break;

            }
        } else {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }
        return false;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    public boolean isToCapture() {
        return toCapture;
    }

    public void setToCapture(boolean toCapture) {
        this.toCapture = toCapture;
    }
}
