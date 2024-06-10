package nro.services;

import nro.consts.ConstMob;
import nro.consts.ConstTask;
import nro.models.boss.BossFactory;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import nro.models.map.ItemMap;
import nro.models.mob.Mob;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.server.io.Message;
import nro.utils.Log;
import nro.utils.Util;

import java.util.ArrayList;
import java.util.List;
import nro.consts.ConstMap;

/**
 * @Build by Arriety
 */
public class MobService {

    private static MobService i;

    private MobService() {

    }

    public static MobService gI() {
        if (i == null) {
            i = new MobService();
        }
        return i;
    }

    public void sendMobStillAliveAffterAttacked(Mob mob, int dameHit, boolean crit, Player player) {
        Message msg;
        try {
            if (player != null) {
                player.checkHutMau();
                msg = new Message(-9);
                msg.writer().writeByte(mob.id);
                msg.writer().writeInt(mob.point.getHP());
                msg.writer().writeInt(dameHit);
                msg.writer().writeBoolean(crit); // chí mạng
                msg.writer().writeByte(player.checkHutMau() ? 37 : -1);
                Service.getInstance().sendMessAllPlayerInMap(mob.zone, msg);
                msg.cleanup();
            }
        } catch (Exception e) {
            Log.error(MobService.class, e);
        }
    }

    public void sendMobDieAffterAttacked(Mob mob, Player plKill, int dameHit) {
        Message msg;
        try {
            msg = new Message(-12);
            msg.writer().writeByte(mob.id);
            msg.writer().writeInt(dameHit);
            msg.writer().writeBoolean(plKill.nPoint.isCrit); // crit
            List<ItemMap> items = mobReward(mob, plKill, msg);
            Service.getInstance().sendMessAllPlayerInMap(mob.zone, msg);
            msg.cleanup();
            hutItem(plKill, items);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hutItem(Player player, List<ItemMap> items) {
        if (!player.isPet) {
            if (player.charms.tdThuHut > System.currentTimeMillis()) {
                for (ItemMap item : items) {
                    if (item.itemTemplate.id != 590) {
                        ItemMapService.gI().pickItem(player, item.itemMapId);
                    }
                }
            }
        } else {
            if (((Pet) player).master.charms.tdThuHut > System.currentTimeMillis()) {
                for (ItemMap item : items) {
                    if (item.itemTemplate.id != 590) {
                        ItemMapService.gI().pickItem(((Pet) player).master, item.itemMapId);
                    }
                }
            }
        }
    }

    private List<ItemMap> mobReward(Mob mob, Player player, Message msg) {
        List<ItemMap> itemReward = new ArrayList<>();
        try {
            itemReward = RewardService.gI().getRewardItems(player, mob,
                    mob.location.x + Util.nextInt(-10, 10), mob.zone.map.yPhysicInTop(mob.location.x, mob.location.y));
            msg.writer().writeByte(itemReward.size()); //sl item roi
            for (ItemMap itemMap : itemReward) {
                msg.writer().writeShort(itemMap.itemMapId);// itemmapid
                msg.writer().writeShort(itemMap.itemTemplate.id); // id item
                msg.writer().writeShort(itemMap.x); // xend item
                msg.writer().writeShort(itemMap.y); // yend item
                msg.writer().writeInt((int) itemMap.playerId); // id nhan nat
            }
        } catch (Exception e) {
            Log.error(MobService.class, e);
        }
        return itemReward;
    }

    public int mobAttackPlayer(Mob mob, Player player) {
        int dameMob = mob.point.getDameAttack();
        if (player.charms.tdDaTrau > System.currentTimeMillis()) {
            dameMob /= 2;
        }
        return player.injured(null, dameMob, false, true);
    }

    public void sendMobAttackMe(Mob mob, Player player, int dame) {
        if (!player.isPet) {
            Message msg;
            try {
                msg = new Message(-11);
                msg.writer().writeByte(mob.id);
                msg.writer().writeInt(dame); //dame
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                Log.error(MobService.class, e);
            }
        }
    }

    public void sendMobAttackPlayer(Mob mob, Player player) {
        Message msg;
        try {
            msg = new Message(-10);
            msg.writer().writeByte(mob.id);
            msg.writer().writeInt((int) player.id);
            msg.writer().writeInt(player.nPoint.hp);
            Service.getInstance().sendMessAnotherNotMeInMap(player, msg);
            msg.cleanup();
        } catch (Exception e) {
            Log.error(MobService.class, e);
        }
    }

    public void hoiSinhMob(Mob mob) {
        boolean isDie = mob.isDie();
        mob.point.hp = mob.point.maxHp;
        mob.setTiemNang();
        if (isDie) {
            Message msg;
            try {
                msg = new Message(-13);
                msg.writer().writeByte(mob.id);
                msg.writer().writeByte(mob.tempId);
                msg.writer().writeByte(0); //level mob
                msg.writer().writeInt((mob.point.hp));
                Service.getInstance().sendMessAllPlayerInMap(mob.zone, msg);
                msg.cleanup();
            } catch (Exception e) {
                Log.error(MobService.class, e);
            }
        }
    }

    public void hoiSinhMobDoanhTrai(Mob mob) {
        if (mob.tempId == ConstMob.BULON) {
            boolean haveTrungUyTrang = false;
            List<Player> bosses = mob.zone.getBosses();
            for (Player boss : bosses) {
                if (boss.id == BossFactory.TRUNG_UY_TRANG) {
                    haveTrungUyTrang = true;
                    break;
                }
            }
            if (haveTrungUyTrang) {
                hoiSinhMob(mob);
            }
        }
    }

    public void initMobDoanhTrai(Mob mob, Clan clan) {
        for (ClanMember cm : clan.getMembers()) {
            for (Player pl : clan.membersInGame) {
                if (pl.id == cm.id && pl.nPoint.hpMax >= mob.point.clanMemHighestHp) {
                    mob.point.clanMemHighestHp = pl.nPoint.hpMax;
                }
            }
        }
        mob.point.dame = mob.point.clanMemHighestHp / mob.point.xHpForDame;
        for (ClanMember cm : clan.getMembers()) {
            for (Player pl : clan.membersInGame) {
                if (pl.id == cm.id && pl.nPoint.dame >= mob.point.clanMemHighestDame) {
                    mob.point.clanMemHighestDame = pl.nPoint.dame;
                }
            }
        }
        mob.point.hp = mob.point.clanMemHighestDame * mob.point.xDameForHp;
    }

    public void initMobDoanhTrai(Mob mob, long point) {
        mob.point.hp = mob.point.maxHp = (int) (point / 10);
        mob.point.dame = mob.point.dame = (int) (point / 200);
    }

    public void initMobBanDoKhoBau(Mob mob, byte level) {
        mob.point.dame = level * 1250 * mob.level * 4;
        mob.point.maxHp = level * 9472 * mob.level * 2 + level * 4263 * mob.tempId;
    }

//    public static void main(String[] args) {
//        int level = 110;
//        int tn = 100;
//        tn += (level / 5 * 50);
//        System.out.println(tn);
//    }
    public void dropItemTask(Player player, Mob mob) {
        ItemMap itemMap = null;
        switch (mob.tempId) {
            case ConstMob.KHUNG_LONG:
            case ConstMob.LON_LOI:
            case ConstMob.QUY_DAT:
                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_2_0) {
                    itemMap = new ItemMap(mob.zone, 73, 1, mob.location.x, mob.location.y, player.id);
                }
                break;
            case 88:
            case 89:
                if (Util.isTrue(1, 100)) {
                    itemMap = new ItemMap(mob.zone, 1098, 1, mob.location.x, mob.location.y, player.id);
                }
                break;
        }

//        switch (player.zone.map.mapId) {
//            case ConstMap.DIA_NGUC_ONE:
//            case ConstMap.DIA_NGUC_TWO: {
//                if (Util.isTrue(1, 100)) {
//                    itemMap = new ItemMap(mob.zone, 1098, 1, mob.location.x, mob.location.y, player.id);
//                }
//            }
//        }
        if (itemMap != null) {
            Service.getInstance().dropItemMap(mob.zone, itemMap);
        }
    }

    public boolean isMonterFly(int tempId) {
        return tempId == ConstMob.QUY_BAY || tempId == ConstMob.QUY_BAY_ME || tempId == ConstMob.QUY_BAY_2 || tempId == ConstMob.THAN_LAN_BAY || tempId == ConstMob.THAN_LAN_BAY_2;
    }
}
