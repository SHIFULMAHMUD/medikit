package com.app.medikit.adapter;

import static com.app.medikit.util.AppExtensions.getString;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.app.medikit.R;
import com.app.medikit.model.Contact;
import com.app.medikit.util.SharedPreference;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private List<Contact>           contacts = new ArrayList<>();
    private final SharedPreference  sp;

    public ContactAdapter() {
        sp = new SharedPreference();
    }

    public void setContacts(List<Contact> contacts) {
        if(contacts != null && contacts.size() != 0) this.contacts = contacts;
        else this.contacts = new ArrayList<>();
        notifyDataSetChanged();
    }

    private Contact getContact(int position){
        return contacts.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.sample_contact, parent, false);
        return new ViewHolder(view);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AppCompatTextView contactName;
        private final AppCompatTextView contactNumber;
        private final AppCompatTextView addContact;
        private final AppCompatTextView removeContact;

        private ViewHolder(View v) {
            super(v);
            contactName = v.findViewById(R.id.name);
            contactNumber = v.findViewById(R.id.number);
            addContact = v.findViewById(R.id.addContact);
            removeContact = v.findViewById(R.id.removeContact);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        Contact emergencyContact = sp.getEmergencyContact();
        if(emergencyContact != null && emergencyContact.getNumber() != null && getContact(position) != null && getContact(position).getNumber() != null){
            String emergencyNumber = emergencyContact.getNumber();
            String currentNumber = getContact(position).getNumber();
            if(emergencyNumber.equals(currentNumber)
                    || ("0" + emergencyNumber).equals(currentNumber)
                    || ("0" + currentNumber).equals(emergencyNumber)
                    || ("880" + emergencyNumber).equals(currentNumber)
                    || ("880" + currentNumber).equals(emergencyNumber)
                    || ("+880" + emergencyNumber).equals(currentNumber)
                    || ("+880" + currentNumber).equals(emergencyNumber)){

                holder.addContact.setVisibility(View.GONE);
                holder.removeContact.setVisibility(View.VISIBLE);
            }
            else {
                holder.addContact.setVisibility(View.VISIBLE);
                holder.removeContact.setVisibility(View.GONE);
            }
        }
        else {
            holder.addContact.setVisibility(View.VISIBLE);
            holder.removeContact.setVisibility(View.GONE);
        }

        holder.contactName.setText(getContact(position).getName() == null ? getString(R.string.unknown) : getContact(position).getName());
        holder.contactNumber.setText(getContact(position).getNumber() == null ? getString(R.string.numberNotFound) : getContact(position).getNumber());

        holder.addContact.setOnClickListener(view -> {
            sp.storeEmergencyContact(getContact(position));
            notifyDataSetChanged();
        });

        holder.removeContact.setOnClickListener(view -> {
            sp.storeEmergencyContact(null);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }
}
