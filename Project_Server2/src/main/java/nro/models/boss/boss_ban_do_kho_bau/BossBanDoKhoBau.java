package nro.models.boss.boss_ban_do_kho_bau;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossManager;
import nro.models.item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.map.phoban.BanDoKhoBau;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.services.Service;
import nro.services.func.ChangeMapService;
import nro.utils.Util;

/**
 *
 * Arriety
 *
 */
public abstract class BossBanDoKhoBau extends Boss {

    protected BanDoKhoBau banDoKhoBau;

    public BossBanDoKhoBau(byte id, BossData data, BanDoKhoBau banDoKhoBau) {
        super(id, data);
        this.banDoKhoBau = banDoKhoBau;
        this.spawn(banDoKhoBau.level);
    }

    private void spawn(byte level) {
        this.nPoint.hpg = level * this.data.hp[0][0];
        switch (this.data.typeDame) {
            case DAME_PERCENT_HP_THOU:
                this.nPoint.dameg = this.nPoint.hpg / 1000 * this.data.dame;
                break;
            case DAME_PERCENT_HP_HUND:
                this.nPoint.dameg = this.nPoint.hpg / 100 * this.data.dame;
                break;
        }
        this.nPoint.calPoint();
        this.nPoint.setFullHpMp();
    }

    @Override
    public void attack() {
        super.attack();
    }

    @Override
    public void idle() {
        boolean allMobDie = true;
        for (Mob mob : this.zone.mobs) {
            if (!mob.isDie()) {
                allMobDie = false;
                break;
            }
        }
        if (allMobDie) {
            this.changeToAttack();
        }
    }

    @Override
    public void checkPlayerDie(Player pl) {
        if (pl.isDie()) {
            Service.getInstance().chat(this, "Chừa chưa ranh con, nên nhớ ta là " + this.name);
        }
    }

    @Override
    public void initTalk() {

    }

    @Override
    public void leaveMap() {
        super.leaveMap();
        BossManager.gI().removeBoss(this);
    }

    @Override
    public void rewards(Player pl) {
        this.dropItemReward(2040, (int) pl.id);
        this.dropItemReward(2040, (int) pl.id);
        this.dropItemReward(2011, (int) pl.id);
    }

    @Override
    protected boolean useSpecialSkill() {
        return false;
    }

    @Override
    protected void notifyPlayeKill(Player player) {
    }

    @Override
    public void joinMap() {
        try {
            this.zone = this.banDoKhoBau.getMapById(mapJoin[Util.nextInt(0, mapJoin.length - 1)]);
            ChangeMapService.gI().changeMap(this, this.zone, 1065, this.zone.map.yPhysicInTop(1065, 0));
        } catch (Exception e) {

        }
    }

}
