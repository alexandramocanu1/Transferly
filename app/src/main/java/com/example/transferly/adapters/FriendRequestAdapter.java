package com.example.transferly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.transferly.R;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    private final List<String> requests;
    private final Context context;
    private final String currentUser;

    public FriendRequestAdapter(List<String> requests, Context context, String currentUser) {
        this.requests = requests;
        this.context = context;
        this.currentUser = currentUser;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_request_item, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        String sender = requests.get(position);
        holder.username.setText(sender);

        holder.acceptButton.setOnClickListener(v -> {
            String url = "http://transferly.go.ro:8080/api/users/acceptRequest";
            Map<String, String> body = new HashMap<>();
            body.put("receiver", currentUser);
            body.put("sender", sender);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(body),
                    response -> {
                        Toast.makeText(context, "Accepted " + sender, Toast.LENGTH_SHORT).show();
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION && pos < requests.size()) {
                            requests.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    },
                    error -> {
                        Toast.makeText(context, "Error accepting " + sender, Toast.LENGTH_SHORT).show();
                        error.printStackTrace();
                    });

            Volley.newRequestQueue(context).add(request);
        });

        holder.declineButton.setOnClickListener(v -> {
            String url = "http://transferly.go.ro:8080/api/users/declineRequest?receiver=" + currentUser + "&sender=" + sender;

            StringRequest request = new StringRequest(Request.Method.DELETE, url,
                    response -> {
                        Toast.makeText(context, "Declined " + sender, Toast.LENGTH_SHORT).show();
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION && pos < requests.size()) {
                            requests.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    },
                    error -> {
                        Toast.makeText(context, "Error declining " + sender, Toast.LENGTH_SHORT).show();
                        error.printStackTrace();
                    });

            Volley.newRequestQueue(context).add(request);
        });
    }


    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        Button acceptButton, declineButton;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.requestUsername);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
        }
    }
}
