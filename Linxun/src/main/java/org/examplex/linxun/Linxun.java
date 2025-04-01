package org.examplex.linxun;
import org.bukkit.Bukkit;       //核心类（必须导入）
import org.bukkit.ChatColor;    //获取聊天框的颜色枚举
import org.bukkit.entity.Item;  //获取掉落物的类
import org.bukkit.World;  //获取所有世界的类
import org.bukkit.command.Command; //获取游戏内指令的类
import org.bukkit.command.CommandSender; //获取输入指令输入者/执行者
import org.bukkit.plugin.java.JavaPlugin;   //核心类（必须导入）
import org.bukkit.scheduler.BukkitTask;
public final class Linxun extends JavaPlugin {
    private boolean CleanStart = true;
    private boolean DoubleConfir = false;
    private int CleanSchedulerID = -1;
    private int CleanCountDownSchedulerID = -1;
    private BukkitTask cleanTask;
    private int CleanCountDown = 5;
    @Override
    public void onEnable() {
        Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW+"已加载凌寻-扫地姬");  // 给服务器控制台发送加载信息
        this.getCommand("linxun_cleanItem").setExecutor(this);//getCommand：注册指令  setExecutor: 在那个插件执行
        OpenClean();
    }

    //操作指令
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (cmd.getName().equalsIgnoreCase("linxun_cleanItem")) {
                if (args.length == 0) {
                    sender.sendMessage(ChatColor.YELLOW + "[系统] 凌寻扫地姬使用帮助:");
                    sender.sendMessage(ChatColor.RED + "/linxun_cleanItem start - 开启定期清理");
                    sender.sendMessage(ChatColor.RED + "/linxun_cleanItem stop - 停止定期清理");
                    sender.sendMessage(ChatColor.RED + "/linxun_cleanItem clean - 立即清理掉落物");
                    sender.sendMessage(ChatColor.RED + "/linxun_cleanItem status - 查看当前状态");
                    return true;
                }
                switch (args[0].toLowerCase()) {
                    case "start":
                        OpenClean(sender);
                        return true;

                    case "stop":
                        CloseClean(sender);
                        return true;

                    case "clean":
                        final CommandSender finalSender = sender;
                        if (args.length == 1){
                            sender.sendMessage(ChatColor.RED + "[系统] 请在10秒内输入/linxun_cleanItem clean qr来确认清理");
                            DoubleConfir = true;
                            cleanTask = Bukkit.getScheduler().runTaskLater(this,()->{
                                try {
                                    DoubleConfir = false;
                                    finalSender.sendMessage(ChatColor.RED + "[系统] 已超时，重新输入/linxun_cleanItem clean");
                                }catch (Exception e){
                                    Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW+"二次确认异常");
                                }
                            },210L);
                            return true;
                        }
                        if (args.length == 2 && args[1].equalsIgnoreCase("qr")) {
                            if (DoubleConfir){
                                cleanTask.cancel();
                                cleanTask = null;
                                DoubleConfir = false;
                                InstantClean();
                            }else {
                                sender.sendMessage(ChatColor.RED + "[系统] 请先输入/linxun_cleanItem clean来进行操作");
                                return true;
                            }
                        }else {
                            DoubleConfir = false;
                            cleanTask.cancel();
                            cleanTask = null;
                            sender.sendMessage(ChatColor.RED + "[系统] 输入有误，请重新输入 /linxun_cleanItem 来进行清理操作");
                        }
                        return true;
                    case "status":
                        sender.sendMessage(ChatColor.YELLOW + "[系统] 凌寻-扫地姬开启状态");
                        sender.sendMessage(CleanStart ?ChatColor.GREEN + "已开启" :ChatColor.RED +"已关闭");
                        return true;

                    default:
                        sender.sendMessage(ChatColor.RED + "[系统] 未知指令，请输入/linxun_cleanItem来查看帮助");
                        return true;
                }
            }
        return false;
    }

    private void OpenClean (CommandSender sender) {
        if (CleanStart) {
            sender.sendMessage(ChatColor.RED + "[系统] 凌寻-扫地姬已是开启状态，请勿重复开启!!!!");
            return;
        }
        OpenClean();
        CleanStart = true;
        sender.sendMessage(ChatColor.YELLOW + "[系统] 凌寻-扫地姬已手动开启");
    }

    private void OpenClean () {CleanSchedulerID = Bukkit.getScheduler().scheduleSyncRepeatingTask(this ,this::CountDown,6000L, 6000L);}

    private void CloseClean (CommandSender sender) {
        if (!CleanStart) {
            sender.sendMessage(ChatColor.RED + "[系统] 凌寻-扫地姬已是关闭状态，请勿重复关闭!!!!");
            return;
        }
        Bukkit.getScheduler().cancelTask(CleanSchedulerID);
        Bukkit.getScheduler().cancelTask(CleanCountDownSchedulerID);
        CleanSchedulerID = -1;
        CleanCountDownSchedulerID = -1;
        CleanStart = false;
        sender.sendMessage(ChatColor.YELLOW + "[系统] 凌寻-扫地姬已手动关闭");
    }

    private void InstantClean () {
        int itemCount = 0;
        for (World World : Bukkit.getWorlds()){
            for (Item item : World.getEntitiesByClass(Item.class)) {
                item.remove();
                itemCount++;
            }
        }
        Bukkit.broadcastMessage(ChatColor.YELLOW+"[系统] 凌寻扫地姬:"+ChatColor.AQUA+"已清理"+itemCount+"个掉落物");
    }

    private void CountDown () {
        if (CleanCountDownSchedulerID !=-1){Bukkit.getScheduler().cancelTask(CleanCountDownSchedulerID);}

        CleanCountDownSchedulerID = Bukkit.getScheduler().scheduleSyncRepeatingTask(this,()->{
            if (CleanCountDown > 0) {
                Bukkit.broadcastMessage(ChatColor.YELLOW +"[系统] 凌寻扫地姬:"+ChatColor.AQUA+"倒计时"+CleanCountDown+"秒清理掉落物");
                CleanCountDown--;
            }else {
                InstantClean();
                CleanCountDown = 5;
                Bukkit.getScheduler().cancelTask(CleanCountDownSchedulerID);
                CleanCountDownSchedulerID = -1;
            }
        },0L,20L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getScheduler().cancelTask(CleanSchedulerID);
        Bukkit.getScheduler().cancelTask(CleanCountDownSchedulerID);
        Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW+"已卸载定期清理掉落物插件");
    }
}
