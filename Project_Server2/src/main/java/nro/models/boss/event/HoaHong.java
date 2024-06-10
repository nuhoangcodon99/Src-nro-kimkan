/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nro.models.boss.event;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossFactory;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.services.Service;
import nro.services.func.ChangeMapService;
import nro.utils.Util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @Build by Arriety
 */
public class HoaHong extends Boss {

    private int mapID;

    public HoaHong(int mapID) {
        super(BossFactory.HOA_HONG, BossData.HOA_HONG);
        this.mapID = mapID;
    }

    @Override
    protected boolean useSpecialSkill() {
        return false;
    }

    @Override
    public void attack() {
    }

    @Override
    public int injured(Player plAtt, int damage, boolean piercing, boolean isMobAttack) {
        if (plAtt.effectFlagBag.useGayTre) {
            damage = 10;
        } else {
            damage = 5;
        }
        return super.injured(plAtt, damage, piercing, isMobAttack);
    }

    @Override
    public void rewards(Player pl) {
        try {
            ItemMap itemMap = new ItemMap(this.zone, 610, 1, pl.location.x, this.zone.map.yPhysicInTop(pl.location.x, pl.location.y - 24), pl.id);
            Service.getInstance().dropItemMap(this.zone, itemMap);
        } catch (Exception ex) {
            Logger.getLogger(HoaHong.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void idle() {

    }

    @Override
    public void checkPlayerDie(Player pl) {

    }

    @Override
    public void initTalk() {

    }

    @Override
    public void leaveMap() {
        super.leaveMap();
        setJustRest();
    }

    @Override
    public void joinMap() {
        this.zone = getMapCanJoin(mapID);
        int x = Util.nextInt(50, this.zone.map.mapWidth - 50);
        ChangeMapService.gI().changeMap(this, this.zone, x, this.zone.map.yPhysicInTop(x, 0));
    }

    @Override
    public void notifyPlayeKill(Player player) {
    }

}
