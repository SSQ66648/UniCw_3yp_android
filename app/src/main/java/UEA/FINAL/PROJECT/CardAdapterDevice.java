/*------------------------------------------------------------------------------
 * PROJECT:     3YP Motorcycle Feedback System Mobile Device App
 *
 * FILE:        CardAdapterDevice.Java
 *
 * LAYOUT(S):   n/a
 *
 * DESCRIPTION: Implementation of own version of exampleAdapter
 *
 * AUTHOR:      SSQ16SHU / 100166648
 *
 * HISTORY:     v1.0    200315  Initial implementation.
*------------------------------------------------------------------------------
 * NOTES:
 *      +
 *------------------------------------------------------------------------------
 * TO DO LIST:
 *      //todo:
 -----------------------------------------------------------------------------*/

package UEA.FINAL.PROJECT;
/*--------------------------------------
    IMPORT LIST
--------------------------------------*/

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CardAdapterDevice extends RecyclerView.Adapter<CardAdapterDevice.CardViewHolder> {
    private static final String TAG = "CardAdapterDevice";

    /*--------------------------------------
        CLASS VARIABLES
    --------------------------------------*/
    //---VARIABLES---
    private ArrayList<DeviceCard> cardList;


    /*--------------------------------------
        CONSTRUCTOR(S)
    --------------------------------------*/
    public CardAdapterDevice(ArrayList<DeviceCard> exampleList) {
        cardList = exampleList;
    }


    /*--------------------------------------
        INNER CLASS(ES)
    --------------------------------------*/
    public static class CardViewHolder extends RecyclerView.ViewHolder {

        //---VIEWS---
        public ImageView deviceIcon;
        public TextView deviceName;

        //---CONSTRUCTOR---
        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            //references to views (pass values in onBindViewHolder)
            deviceIcon = itemView.findViewById(R.id.image_carddevice_icon);
            deviceName = itemView.findViewById(R.id.labeltext_carddevice_device_name);
        }
    }


    /*--------------------------------------
        CREATE
    --------------------------------------*/
    @NonNull
    @Override
    public CardAdapterDevice.CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_device, parent, false);
        CardAdapterDevice.CardViewHolder cardViewHolder = new CardAdapterDevice.CardViewHolder(v);
        return cardViewHolder;
    }


    /*--------------------------------------
        LISTENERS
    --------------------------------------*/
    @Override
    public void onBindViewHolder(@NonNull CardAdapterDevice.CardViewHolder holder, int position) {
        //pass information to item currently looked at (position)
        DeviceCard currentItem = cardList.get(position); //item at position
        //get info (image (in holder) changed to image returned by ArrayList item (currentItem))
        holder.deviceIcon.setImageResource(currentItem.getImageResource());
        holder.deviceName.setText(currentItem.getDeviceName());
    }


    /*--------------------------------------
        METHODS
    --------------------------------------*/
    //-get number of items in list
    @Override
    public int getItemCount() {
        return cardList
                .size();
    }


    //-replace the list with new version
    //todo: test this
    public void updateDataset(ArrayList<DeviceCard> newList) {
        cardList
                .clear();
        cardList
                .addAll(newList);
        this.notifyDataSetChanged();
    }


}
