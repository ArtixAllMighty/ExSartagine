package subaraki.exsartagine.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityPan extends TileEntity implements ITickable {

	private boolean isCooking = false;
	private int cookingTime = 0;

	private ItemStackHandler inventory = new ItemStackHandler(2){
		@Override
		protected void onContentsChanged(int slot)
		{
			super.onContentsChanged(slot);
			markDirty();
		}
	};

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return true;
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return (T) inventory;
		}
		return super.getCapability(capability, facing);
	}

	public ItemStackHandler getInventory(){
		return inventory;
	}

	public void setCooking(){
		isCooking = true;
	}

	public void stopCooking(){
		isCooking = false;
		cookingTime = 0;
	}
	
	public boolean isCooking(){
		return isCooking;
	}

	@Override
	public void update() {
		if(cookingTime == 200)
		{
			if(inventory.getStackInSlot(1).isEmpty() || inventory.getStackInSlot(1).getCount() < inventory.getStackInSlot(1).getMaxStackSize()){
				inventory.getStackInSlot(0).shrink(1);
				if(inventory.getStackInSlot(1).isEmpty())
					inventory.setStackInSlot(1, FurnaceRecipes.instance().getSmeltingResult(inventory.getStackInSlot(0).copy()));
				else
					inventory.getStackInSlot(1).grow(1);
			}
			cookingTime = 0;
		}
		if(isCooking && (inventory.getStackInSlot(1).isEmpty() || inventory.getStackInSlot(1).getCount() < inventory.getStackInSlot(1).getMaxStackSize()))
			cookingTime++;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setInteger("cooktime", cookingTime);
		compound.setBoolean("cooking", isCooking);
		compound.setTag("inv", inventory.serializeNBT());
		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		if(compound.hasKey("cooktime") && compound.hasKey("cooking")){
			this.isCooking = compound.getBoolean("cooking");
			this.cookingTime = compound.getInteger("cooktime");
		}
		if(compound.hasKey("inv"))
			inventory.deserializeNBT(compound.getCompoundTag("inv"));
	}

	/////////////////3 METHODS ABSOLUTELY NEEDED FOR CLIENT/SERVER SYNCING/////////////////////
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		this.writeToNBT(nbt);

		return new SPacketUpdateTileEntity(getPos(), 0, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		this.readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound nbt =  super.getUpdateTag();
		writeToNBT(nbt);
		return nbt;
	}

	//calls readFromNbt by default. no need to add anything in here
	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.handleUpdateTag(tag);
	}
	////////////////////////////////////////////////////////////////////

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate)
	{
		return oldState.getBlock() != newSate.getBlock();
	}
}
