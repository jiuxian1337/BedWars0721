package cc.bw0721.transformer;

import cc.bw0721.BedWars0721;
import cc.bw0721.asm.ASMTransformer;
import cc.bw0721.utils.XPUtils;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.shop.IContentTier;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.shop.main.CategoryContent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class CategoryContentTransformer extends ASMTransformer {
    public CategoryContentTransformer() {
        super(CategoryContent.class);
    }

    @Inject(method = "calculateMoney", desc = "(Lorg/bukkit/entity/Player;Lorg/bukkit/Material;)I")
    public void hookCalculateMoneyBody(MethodNode method) {
        InsnList body = new InsnList();
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                Type.getInternalName(CategoryContentTransformer.class),
                "hookCalculateMoney",
                "(Lorg/bukkit/entity/Player;Lorg/bukkit/Material;)I",
                false));
        body.add(new InsnNode(Opcodes.IRETURN));
        replaceBody(method, body);
    }

    @Inject(method = "takeMoney", desc = "(Lorg/bukkit/entity/Player;Lorg/bukkit/Material;I)V")
    public void hookTakeMoneyBody(MethodNode method) {
        InsnList body = new InsnList();
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new VarInsnNode(Opcodes.ILOAD, 2));
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                Type.getInternalName(CategoryContentTransformer.class),
                "hookTakeMoney",
                "(Lorg/bukkit/entity/Player;Lorg/bukkit/Material;I)V",
                false));
        body.add(new InsnNode(Opcodes.RETURN));
        replaceBody(method, body);
    }

    @Inject(method = "execute", desc = "(Lorg/bukkit/entity/Player;Lcom/andrei1058/bedwars/shop/ShopCache;I)V")
    public void hookExecute(MethodNode execute) {
        for (AbstractInsnNode insn : execute.instructions) {
            if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode method = (MethodInsnNode) insn;
                if (method.owner.equals("com/andrei1058/bedwars/shop/main/CategoryContent")) {
                    if (method.name.equals("calculateMoney")) {
                        method.owner = Type.getInternalName(CategoryContentTransformer.class);
                        method.name = "hookCalculateMoney";
                    } else if (method.name.equals("takeMoney")) {
                        method.owner = Type.getInternalName(CategoryContentTransformer.class);
                        method.name = "hookTakeMoney";
                    }
                }
            }
        }

        AbstractInsnNode sendMessageInsn = null;
        for (AbstractInsnNode insn : execute.instructions) {
            if (insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                MethodInsnNode m = (MethodInsnNode) insn;
                if (m.name.equals("sendMessage") && m.owner.equals("org/bukkit/entity/Player")) {
                    AbstractInsnNode cur = insn.getPrevious();
                    boolean found = false;
                    int depth = 0;
                    while (cur != null && depth < 50) {
                        if (cur.getOpcode() == Opcodes.GETSTATIC) {
                            FieldInsnNode field = (FieldInsnNode) cur;
                            if (field.name.equals("SHOP_INSUFFICIENT_MONEY")) {
                                found = true;
                                break;
                            }
                        }
                        cur = cur.getPrevious();
                        depth++;
                    }
                    if (found) {
                        sendMessageInsn = insn;
                        break;
                    }
                }
            }
        }

        if (sendMessageInsn != null) {
            AbstractInsnNode start = null;
            AbstractInsnNode cur = sendMessageInsn.getPrevious();
            while (cur != null) {
                if (cur.getOpcode() == Opcodes.GETSTATIC) {
                    FieldInsnNode field = (FieldInsnNode) cur;
                    if (field.name.equals("SHOP_INSUFFICIENT_MONEY")) {
                        AbstractInsnNode prev = cur.getPrevious();
                        int aloadCount = 0;
                        while (prev != null) {
                            if (prev.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) prev).var == 1) {
                                aloadCount++;
                                if (aloadCount == 2) {
                                    start = prev;
                                    break;
                                }
                            }
                            prev = prev.getPrevious();
                        }
                        break;
                    }
                }
                cur = cur.getPrevious();
            }

            if (start != null) {
                InsnList replacement = new InsnList();
                replacement.add(new VarInsnNode(Opcodes.ALOAD, 1));
                replacement.add(new VarInsnNode(Opcodes.ALOAD, 4));
                replacement.add(new VarInsnNode(Opcodes.ILOAD, 5));
                replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        Type.getInternalName(CategoryContentTransformer.class),
                        "hookCantBuy",
                        "(Lorg/bukkit/entity/Player;Lcom/andrei1058/bedwars/api/arena/shop/IContentTier;I)V",
                        false));

                execute.instructions.insertBefore(start, replacement);

                AbstractInsnNode delCur = start;
                while (delCur != null) {
                    AbstractInsnNode next = delCur.getNext();
                    execute.instructions.remove(delCur);
                    if (delCur == sendMessageInsn) break;
                    delCur = next;
                }
            }
        }
    }

    @Inject(method = "getItemStack", desc = "(Lorg/bukkit/entity/Player;Lcom/andrei1058/bedwars/shop/ShopCache;)Lorg/bukkit/inventory/ItemStack;")
    public void hookGetItemStack(MethodNode method) {
        int getPriceCount = 0;

        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode insn = method.instructions.get(i);

            if (insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                MethodInsnNode m = (MethodInsnNode) insn;
                if (m.name.equals("getPrice") && m.owner.equals("com/andrei1058/bedwars/api/arena/shop/IContentTier")) {
                    getPriceCount++;
                    if (getPriceCount == 2) {
                        method.instructions.insertBefore(insn.getPrevious(), new VarInsnNode(Opcodes.ALOAD, 1));
                        m.setOpcode(Opcodes.INVOKESTATIC);
                        m.owner = Type.getInternalName(CategoryContentTransformer.class);
                        m.name = "hookGetPrice";
                        m.desc = "(Lorg/bukkit/entity/Player;Lcom/andrei1058/bedwars/api/arena/shop/IContentTier;)Ljava/lang/String;";
                        m.itf = false;
                        AbstractInsnNode next = insn.getNext();
                        if (next != null && next.getOpcode() == Opcodes.INVOKESTATIC) {
                            MethodInsnNode nextM = (MethodInsnNode) next;
                            if (nextM.name.equals("valueOf") && nextM.owner.equals("java/lang/String")
                                    && nextM.desc.equals("(I)Ljava/lang/String;")) {
                                method.instructions.remove(nextM);
                            }
                        }
                    }
                }
            }

            if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode m = (MethodInsnNode) insn;
                if (m.owner.equals("java/lang/String") && m.name.equals("valueOf")
                        && m.desc.equals("(Ljava/lang/Object;)Ljava/lang/String;")) {
                    AbstractInsnNode prev = insn.getPrevious();
                    if (prev instanceof VarInsnNode && prev.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) prev).var == 11) {
                        method.instructions.insertBefore(prev, new VarInsnNode(Opcodes.ALOAD, 1));
                        m.owner = Type.getInternalName(CategoryContentTransformer.class);
                        m.name = "hookGetCurrencyColor";
                        m.desc = "(Lorg/bukkit/entity/Player;Ljava/lang/Object;)Ljava/lang/String;";
                    }
                }
                if (m.owner.equals("com/andrei1058/bedwars/shop/main/CategoryContent")) {
                    if (m.name.equals("getCurrencyMsgPath")) {
                        m.owner = Type.getInternalName(CategoryContentTransformer.class);
                        m.name = "hookGetTranslatedCurrency";
                        m.desc = "(Lorg/bukkit/entity/Player;Lcom/andrei1058/bedwars/api/arena/shop/IContentTier;)Ljava/lang/String;";
                        AbstractInsnNode next = insn.getNext();
                        if (next != null && next.getOpcode() == Opcodes.INVOKESTATIC) {
                            MethodInsnNode nextM = (MethodInsnNode) next;
                            if (nextM.name.equals("getMsg") && nextM.owner.equals("com/andrei1058/bedwars/api/language/Language")) {
                                method.instructions.remove(nextM);
                            }
                        }
                    } else if (m.name.equals("calculateMoney")) {
                        m.owner = Type.getInternalName(CategoryContentTransformer.class);
                        m.name = "hookCalculateMoney";
                    }
                }
            }
        }
    }

    private static void replaceBody(MethodNode method, InsnList body) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        method.instructions.add(body);
    }

    private static boolean isXpArena(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        return arena != null && XPUtils.isXPArena(arena.getArenaName());
    }

    private static boolean usesXp(Player player, Material currency) {
        return isXpArena(player) && XPUtils.getExp(currency) > 0;
    }

    private static String expMsg() {
        return BedWars0721.getInstance().getConfigManager().getMainConfig().getExpMsg();
    }

    public static int hookCalculateMoney(Player player, Material currency) {
        if (usesXp(player, currency)) {
            return player.getLevel() / XPUtils.getExp(currency);
        }
        return countCurrency(player, currency);
    }

    private static int countCurrency(Player player, Material currency) {
        if (currency == Material.AIR) {
            return (int) BedWars.getEconomy().getMoney(player);
        }
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (item.getType() == currency) amount += item.getAmount();
        }
        return amount;
    }

    public static void hookTakeMoney(Player player, Material currency, int amount) {
        if (usesXp(player, currency)) {
            player.setLevel(player.getLevel() - XPUtils.getExp(currency) * amount);
            return;
        }
        takeCurrency(player, currency, amount);
    }

    private static void takeCurrency(Player player, Material currency, int amount) {
        if (currency == Material.AIR) {
            if (!BedWars.getEconomy().isEconomy()) {
                player.sendMessage("§4§lERROR: This requires Vault Support! Please install Vault plugin!");
                return;
            }
            BedWars.getEconomy().buyAction(player, amount);
            return;
        }

        int cost = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (item.getType() != currency) continue;
            if (item.getAmount() < cost) {
                cost -= item.getAmount();
                BedWars.nms.minusAmount(player, item, item.getAmount());
                player.updateInventory();
            } else {
                BedWars.nms.minusAmount(player, item, cost);
                player.updateInventory();
                break;
            }
        }
    }

    public static void hookCantBuy(Player player, IContentTier ct, int money) {
        int exp = XPUtils.getExp(ct.getCurrency());
        boolean useXp = usesXp(player, ct.getCurrency());
        String currency = useXp ? expMsg() : Language.getMsg(player, CategoryContent.getCurrencyMsgPath(ct));
        int missing = useXp ? exp * ct.getPrice() - player.getLevel() : ct.getPrice() - money;
        player.sendMessage(Language.getMsg(player, Messages.SHOP_INSUFFICIENT_MONEY)
                .replace("{currency}", currency)
                .replace("{amount}", String.valueOf(missing)));
    }

    public static String hookGetPrice(Player player, IContentTier ct) {
        int exp = XPUtils.getExp(ct.getCurrency());
        if (isXpArena(player) && exp > 0) {
            return String.valueOf(exp * ct.getPrice());
        }
        return String.valueOf(ct.getPrice());
    }

    public static String hookGetTranslatedCurrency(Player player, IContentTier ct) {
        if (usesXp(player, ct.getCurrency())) {
            return expMsg();
        }
        return Language.getMsg(player, CategoryContent.getCurrencyMsgPath(ct));
    }

    public static String hookGetCurrencyColor(Player player, Object color) {
        if (isXpArena(player)) {
            return ChatColor.getByChar(BedWars0721.getInstance().getConfigManager().getMainConfig().getExpColor()).toString();
        }
        return String.valueOf(color);
    }
}
