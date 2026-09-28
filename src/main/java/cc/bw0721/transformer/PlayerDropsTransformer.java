package cc.bw0721.transformer;

import cc.bw0721.asm.ASMTransformer;
import cc.bw0721.utils.XPUtils;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.listeners.dropshandler.PlayerDrops;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class PlayerDropsTransformer extends ASMTransformer {
    public PlayerDropsTransformer() {
        super(PlayerDrops.class);
    }

    @Inject(method = "handlePlayerDrops", desc = "(Lcom/andrei1058/bedwars/api/arena/IArena;Lorg/bukkit/entity/Player;Lorg/bukkit/entity/Player;Lcom/andrei1058/bedwars/api/arena/team/ITeam;Lcom/andrei1058/bedwars/api/arena/team/ITeam;Lcom/andrei1058/bedwars/api/events/player/PlayerKillEvent$PlayerKillCause;Ljava/util/List;)Z")
    public void hookHandlePlayerDrops(MethodNode method) {
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode insn = method.instructions.get(i);
            if (!(insn instanceof MethodInsnNode)) continue;
            MethodInsnNode m = (MethodInsnNode) insn;
            if (!m.owner.equals("org/bukkit/World")) continue;
            if (!m.name.equals("dropItemNaturally")) continue;

            AbstractInsnNode pop = skipFrames(insn.getNext());
            if (pop == null || pop.getOpcode() != Opcodes.POP) continue;

            AbstractInsnNode loopBack = skipFrames(pop.getNext());
            if (loopBack == null || loopBack.getOpcode() != Opcodes.GOTO) continue;

            AbstractInsnNode loopExit = skipFrames(loopBack.getNext());
            if (loopExit == null || loopExit.getOpcode() != Opcodes.GOTO) continue;

            InsnList insert = new InsnList();
            insert.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insert.add(new VarInsnNode(Opcodes.ALOAD, 1));
            insert.add(new VarInsnNode(Opcodes.ALOAD, 3));
            insert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    Type.getInternalName(PlayerDropsTransformer.class),
                    "handleDeathLootDrop",
                    "(Lcom/andrei1058/bedwars/api/arena/IArena;Lorg/bukkit/entity/Player;Lcom/andrei1058/bedwars/api/arena/team/ITeam;)V",
                    false));
            method.instructions.insertBefore(loopExit, insert);
            return;
        }
    }

    @Inject(method = "dropItems", desc = "(Lorg/bukkit/entity/Player;Ljava/util/List;)V")
    public void hookDropItems(MethodNode method) {
        AbstractInsnNode lastReturn = null;
        for (AbstractInsnNode insn : method.instructions) {
            if (insn.getOpcode() == Opcodes.RETURN) lastReturn = insn;
        }
        if (lastReturn == null) return;

        InsnList insert = new InsnList();
        insert.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                Type.getInternalName(PlayerDropsTransformer.class),
                "handleDropItems",
                "(Lorg/bukkit/entity/Player;)V",
                false));
        method.instructions.insertBefore(lastReturn, insert);
    }

    private static AbstractInsnNode skipFrames(AbstractInsnNode node) {
        while (node != null && (node instanceof FrameNode || node instanceof LabelNode || node instanceof LineNumberNode)) {
            node = node.getNext();
        }
        return node;
    }

    public static void handleDeathLootDrop(IArena arena, Player victim, ITeam victimsTeam) {
        if (arena == null || victimsTeam == null) return;
        if (!XPUtils.isXPArena(arena.getArenaName())) return;
        Vector v = victimsTeam.getKillDropsLocation();
        if (v == null || arena.getWorld() == null) return;
        dropExpBottles(victim, new Location(arena.getWorld(), v.getX(), v.getY(), v.getZ()));
    }

    public static void handleDropItems(Player victim) {
        if (victim == null) return;
        IArena arena = Arena.getArenaByPlayer(victim);
        if (arena == null || !XPUtils.isXPArena(arena.getArenaName())) return;
        dropExpBottles(victim, victim.getLocation());
    }

    private static void dropExpBottles(Player victim, Location location) {
        if (location == null || location.getWorld() == null) return;
        int amount = bottleCount(victim.getLevel(), XPUtils.getExp(XPUtils.getExpBottleMaterial()));
        if (amount <= 0) return;
        location.getWorld().dropItemNaturally(location, new ItemStack(XPUtils.getExpBottleMaterial(), amount));
    }

    static int bottleCount(int level, int bottleValue) {
        if (level <= 0) return 0;
        return level / (bottleValue > 0 ? bottleValue : 10);
    }
}
