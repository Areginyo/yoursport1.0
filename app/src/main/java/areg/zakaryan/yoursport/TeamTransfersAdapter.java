package areg.zakaryan.yoursport;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeamTransfersAdapter extends RecyclerView.Adapter<TeamTransfersAdapter.TransferViewHolder> {

    private List<TeamTransfersFragment.TransferItem> transferItems = new ArrayList<>();

    public void setTransfers(List<TeamTransfersFragment.TransferItem> transferItems) {
        this.transferItems = transferItems != null ? transferItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_transfer, parent, false);
        return new TransferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransferViewHolder holder, int position) {
        TeamTransfersFragment.TransferItem item = transferItems.get(position);
        
        holder.txtPlayerName.setText(item.playerName);
        holder.txtLeagueName.setText(item.leagueName);

if (item.transferDate != null && !item.transferDate.isEmpty()) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
                Date date = inputFormat.parse(item.transferDate);
                holder.txtTransferDate.setText(outputFormat.format(date));
            } catch (Exception e) {
                holder.txtTransferDate.setText(item.transferDate);
            }
        } else {
            holder.txtTransferDate.setText("");
        }

if (item.isIncoming()) {
            holder.txtTransferType.setText("IN");
            holder.txtTransferType.setBackgroundColor(0xFF4CAF50); 
            holder.txtTransferType.setTextColor(0xFF181818);
        } else if (item.isOutgoing()) {
            holder.txtTransferType.setText("OUT");
            holder.txtTransferType.setBackgroundColor(0xFFF44336); 
            holder.txtTransferType.setTextColor(0xFFF0F0F0);
        } else {
            holder.txtTransferType.setText("LOAN");
            holder.txtTransferType.setBackgroundColor(0xFFFF9800); 
            holder.txtTransferType.setTextColor(0xFF181818);
        }

if (item.playerPhoto != null && !item.playerPhoto.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.playerPhoto)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(holder.imgPlayerPhoto);
        } else {
            holder.imgPlayerPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
        }

if (item.fromTeamLogo != null && !item.fromTeamLogo.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.fromTeamLogo)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(holder.imgFromTeam);
        } else {
            holder.imgFromTeam.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        
        if (item.toTeamLogo != null && !item.toTeamLogo.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.toTeamLogo)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(holder.imgToTeam);
        } else {
            holder.imgToTeam.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        
        holder.txtFromTeam.setText(item.fromTeamName);
        holder.txtToTeam.setText(item.toTeamName);

if (item.isLoan()) {
            holder.txtLoanIndicator.setVisibility(View.VISIBLE);
            holder.txtLoanIndicator.setText("LOAN");
        } else {
            holder.txtLoanIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return transferItems.size();
    }

    static class TransferViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPlayerPhoto;
        TextView txtPlayerName;
        TextView txtTransferType;
        TextView txtTransferDate;
        TextView txtLeagueName;
        ImageView imgFromTeam;
        TextView txtFromTeam;
        ImageView imgToTeam;
        TextView txtToTeam;
        TextView txtLoanIndicator;

        TransferViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPlayerPhoto = itemView.findViewById(R.id.imgPlayerPhoto);
            txtPlayerName = itemView.findViewById(R.id.txtPlayerName);
            txtTransferType = itemView.findViewById(R.id.txtTransferType);
            txtTransferDate = itemView.findViewById(R.id.txtTransferDate);
            txtLeagueName = itemView.findViewById(R.id.txtLeagueName);
            imgFromTeam = itemView.findViewById(R.id.imgFromTeam);
            txtFromTeam = itemView.findViewById(R.id.txtFromTeam);
            imgToTeam = itemView.findViewById(R.id.imgToTeam);
            txtToTeam = itemView.findViewById(R.id.txtToTeam);
            txtLoanIndicator = itemView.findViewById(R.id.txtLoanIndicator);
        }
    }
}
