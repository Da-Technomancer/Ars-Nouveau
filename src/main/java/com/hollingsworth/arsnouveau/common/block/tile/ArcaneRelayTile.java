package com.hollingsworth.arsnouveau.common.block.tile;

import com.hollingsworth.arsnouveau.api.client.ITooltipProvider;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.NBTUtil;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import com.hollingsworth.arsnouveau.setup.BlockRegistry;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.common.network.PacketANEffect;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;

public class ArcaneRelayTile extends AbstractManaTile implements ITooltipProvider {

    public ArcaneRelayTile() {
        super(BlockRegistry.ARCANE_RELAY_TILE);
    }
    public ArcaneRelayTile(TileEntityType<?> type){
        super(type);
    }

    private BlockPos toPos;
    private BlockPos fromPos;


    public boolean setTakeFrom(BlockPos pos){
        if(BlockUtil.distanceFrom(pos, this.pos) > 10){
            return false;
        }
        this.fromPos = pos;
        update();
        return true;
    }

    public boolean setSendTo(BlockPos pos ){
        if(BlockUtil.distanceFrom(pos, this.pos) > 10){
            return false;
        }
        this.toPos = pos;
        update();
        return true;
    }

    public void clearPos(){
        this.toPos = null;
        this.fromPos = null;
          update();
    }

    @Override
    public int getTransferRate() {
        return 100;
    }

    @Override
    public int getMaxMana() {
        return 200;
    }

    public boolean closeEnough(BlockPos pos, int distance){
        return BlockUtil.distanceFrom(pos, this.pos) <= distance;
    }

    @Override
    public void tick() {
        if(world.isRemote){
            return;
        }

        if(world.getGameTime() % 20 != 0 || toPos == null)
            return;

        if(fromPos != null){
            // Block has been removed
            if(!(world.getTileEntity(fromPos) instanceof AbstractManaTile)){
                fromPos = null;
                world.notifyBlockUpdate(this.pos, world.getBlockState(pos),  world.getBlockState(pos), 2);
                return;
            }else if(world.getTileEntity(fromPos) instanceof AbstractManaTile){
                // Transfer mana fromPos to this
                AbstractManaTile fromTile = (AbstractManaTile) world.getTileEntity(fromPos);
                if(fromTile.getCurrentMana() >= this.getTransferRate() && this.getCurrentMana() + this.getTransferRate() <= this.getMaxMana()){
                    fromTile.removeMana(this.getTransferRate());
                    this.addMana(this.getTransferRate());
                    spawnParticles(fromPos, pos);
                }
            }
        }
        if(!(world.getTileEntity(toPos) instanceof AbstractManaTile)){
            toPos = null;
            update();

            return;
        }
        AbstractManaTile toTile = (AbstractManaTile) this.world.getTileEntity(toPos);
        if(this.getCurrentMana() >= this.getTransferRate() && toTile.getCurrentMana() + this.getTransferRate() <= toTile.getMaxMana()){
            this.removeMana(this.getTransferRate());
            toTile.addMana(this.getTransferRate());
            spawnParticles(pos, toPos);
         }
    }

    @Override
    public void read(BlockState state, CompoundNBT tag) {
        if(NBTUtil.hasBlockPos(tag, "to")){
            this.toPos = NBTUtil.getBlockPos(tag, "to");
        }
        if(NBTUtil.hasBlockPos(tag, "from")){
            this.fromPos = NBTUtil.getBlockPos(tag, "from");
        }
        super.read(state, tag);
    }

    public void spawnParticles(BlockPos from, BlockPos to){
        EntityFollowProjectile aoeProjectile = new EntityFollowProjectile(world, from, to);
        world.addEntity(aoeProjectile);
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        if(toPos != null)
            NBTUtil.storeBlockPos(tag, "to", toPos);
        if(fromPos != null)
            NBTUtil.storeBlockPos(tag, "from", fromPos);
        return super.write(tag);
    }

    @Override
    public List<String> getTooltip() {
        List<String> list = new ArrayList<>();
        if(toPos == null){
            list.add(new TranslationTextComponent("ars_nouveau.relay.no_to").getString());
        }else{
            list.add(new TranslationTextComponent("ars_nouveau.relay.one_to", 1).getString());
        }
        if(fromPos == null){
            list.add(new TranslationTextComponent("ars_nouveau.relay.no_from").getString());
        }else{
            list.add(new TranslationTextComponent("ars_nouveau.relay.one_from", 1).getString());
        }
        return list;
    }
}
