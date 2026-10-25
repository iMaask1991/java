package net.sf.l2j.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.AgathionSkillData;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PetDelete;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

public class Agathion extends Npc
{
   private Player _owner;
   private Future<?> _followTask;
      
   public Agathion(int objectId, NpcTemplate template, Player owner)
   {
       super(objectId, template);
       
        // Set the magical circle animation.
       setShowSummonAnimation(true);
       
       // Set the Player owner.
       _owner = owner;
   }
   
   @Override
   public void onSpawn()
   {
	   broadcastPacket(new MagicSkillUse(this, this, Config.ANIMATION_SPAWN_SKILL_ID_AGATHION, 1, 500, 0));
       followToOwner();
       super.onSpawn();
   }
   
   public void unSummon(Player owner)
   {
       // Abort attack, cast and move.
		abortAttack();
		abortCast();
		stopMove(null);

       owner.sendPacket(new PetDelete(2, getObjectId()));
       owner.setAgathion(null);
       
       if (owner.getSummon() != null)
           owner.getSummon().sendInfo(owner);
       owner.broadcastUserInfo();
       
       if (_followTask != null)
           _followTask.cancel(true);
       
       decayMe();
       deleteMe();
       
       AgathionSkillData.getInstance().removeSkill(owner, getNpcId());
       
       super.deleteMe();
   }
   
   public void followToOwner()
   {
       if (_followTask == null)
           _followTask = ThreadPool.scheduleAtFixedRate(new Follow(this), 1000, 1000);
       
       SpawnLocation loc = _owner.getPosition();
       final int rnd = Rnd.get(-30, +30);
       final boolean action = Rnd.get(100) < 15;

       if (!checkIfInRange(2000, this, _owner))
           teleportTo(_owner.getPosition(), 30);
       else if (!checkIfInRange(1000, this, _owner))
           instantTeleportTo(_owner.getPosition(), 30);
       
       if (_owner.isMoving())
       	   getAI().setIntention(IntentionType.FOLLOW, _owner, false);
       else
       {
           if (checkIfInRange(30, this, _owner))
        	   getAI().setIntention(IntentionType.IDLE);
           if (!Config.AGATHION_ONLY_FOLLOW && (!checkIfInRange(75, this, _owner) || action))
           	   getAI().setIntention(IntentionType.MOVE_TO, new Location(loc.getX()+rnd, loc.getY()+rnd, loc.getZ()), null);
           if (Config.AGATHION_HEAL_OWNER && !action && !isCastingNow())
           {
               if (_owner.isInOlympiadMode() || _owner.isInDuel() || _owner.getPvpFlag() != 0 || _owner.isInsideZone(ZoneId.PVP) || _owner.getKarma() != 0)
                   return;
               
               if ((_owner.getCurrentHp() <= getMaxHp() * Config.AGATHION_PERCENT_TO_HEAL))
               {
            	   setTarget(_owner);
               	   doCast(SkillTable.getInstance().getInfo(Config.SKILL_ID_AGATHION, Config.SKILL_LEVEL_AGATHION));
               }
           }
           else if (!Config.AGATHION_ONLY_FOLLOW && Config.AGATHION_ALLOW_SOCIAL && !isMoving() && action && !isCastingNow())
               broadcastPacket(new SocialAction(this, 1));
       }
   }

   private static class Follow implements Runnable
   {
       private final Agathion agathion;
       
       protected Follow(Agathion aga)
       {
           agathion = aga;
       }
       
       @Override
       public void run()
       {
           agathion.followToOwner();
       }   
   }
   
   public static boolean checkIfInRange(int range, Npc npc, Player player)
   {
       return MathUtil.checkIfInRange((int) (range + npc.getCollisionRadius()), npc, player, true);
   }

}
