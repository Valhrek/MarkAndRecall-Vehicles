package com.pveplands.markandrecallvehicles;

import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;

public class RuneBehaviour implements BehaviourProvider, ModAction {
    private Options options;
    
    public RuneBehaviour(Options ops) {
        options = ops;
    }
    
    @Override
    public List<ActionEntry> getBehavioursFor(@Nonnull Creature performer, @Nonnull Item activated, @Nonnull Item target) {
        return getBehavioursFor(performer, target);
    }
    
    @Override
    public List<ActionEntry> getBehavioursFor(@Nonnull Creature performer, @Nonnull Item target) {
        if (target.getTemplateId() != MarkAndRecallVehicles.getRuneTemplateId())
            return null;
        
        short num = -2;
        
        if (performer.getPower() >= options.getReloadConfigPower())
            num--;
        
        if (performer.getPower() >= options.getConfigInterfacePower())
            num--;
        
        if (!options.isAllowRemark() && target.getData1() != -1)
            num++;
        
        if (target.getData1() == -1)
            num++;
        
        List<ActionEntry> list = new ArrayList<>();
        list.add(new ActionEntry((short)num, "Rune", "Rune"));
        
        // Only add mark option if rune is unmarked, or remarking is allowed.
        if (options.isAllowRemark() || target.getData1() == -1)
            list.add(MarkAndRecallVehicles.getMarkAction().getActionEntry());
        
        if (target.getData1() != -1)
            list.add(MarkAndRecallVehicles.getRecallAction().getActionEntry());
        
        if (performer.getPower() >= options.getReloadConfigPower())
            list.add(MarkAndRecallVehicles.getReloadAction().getActionEntry());
        
        if (performer.getPower() >= options.getConfigInterfacePower())
            list.add(MarkAndRecallVehicles.getInterfaceAction().getActionEntry());
        
        return list;
    }
}
