package com.example.Travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class InsertActivity extends AppCompatActivity {


    private FirebaseDatabase mFirebaseDatabase;
    private static final int PICTURE_VALUE=42;
    private DatabaseReference mDatabaseReference;
    EditText txtTitle;
    EditText txtDescription;
    EditText txtPrice;
    TravelDeal deal;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
     //   FirebaseUtils.openFbRef("traveldeals", this);

        mFirebaseDatabase = FirebaseUtils.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtils.mDatabaseReference;

        txtTitle= (EditText) findViewById(R.id.txtTitle);
        txtPrice=(EditText)findViewById(R.id.txtPrice);
        txtDescription= (EditText) findViewById(R.id.txtDescription);
        imageView = (ImageView) findViewById(R.id.image);

        Intent intent = getIntent();

        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");

        if (deal == null){

            deal = new TravelDeal();
        }

        this.deal= deal;
        txtTitle.setText(deal.getTitle());
        txtPrice.setText(deal.getPrice());
        txtDescription.setText(deal.getDescription());
        showImage(deal.getImageURL());

        Button btnImage = (Button) findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent, "Insert Picture"), PICTURE_VALUE);

            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this, "Deal Saved", Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return true;


            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deleted Successfully", Toast.LENGTH_LONG).show();
                backToList();
                return true;


                default:

                    return super.onOptionsItemSelected(item);

        }



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==PICTURE_VALUE && resultCode == RESULT_OK ){
            Uri imageUri = data.getData();
            StorageReference ref = FirebaseUtils.mStorageRef.child(imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    String url= taskSnapshot.getStorage().getDownloadUrl().toString();
                    String pictureName= taskSnapshot.getStorage().getPath();

                    deal.setImageURL(url);
                    deal.setImageName(pictureName);
                    showImage(url);

                }
            });

        }
    }

    private void clean() {

        txtTitle.setText("");
        txtDescription.setText("");
        txtPrice.setText("");
        txtTitle.requestFocus();

    }

    private void saveDeal() {

        deal.setTitle(txtTitle.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        deal.setDescription(txtDescription.getText().toString());


        if (deal.getId() == null) {

            mDatabaseReference.push().setValue(deal);

        } else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }
    }

    private void deleteDeal(){

        if(deal == null){

            Toast.makeText(this, "Please Save Deal before deleting", Toast.LENGTH_SHORT).show();
            return;

        }else {
            mDatabaseReference.child(deal.getId()).removeValue();

            if (deal.getImageName() != null && deal.getImageName().isEmpty()==false){

                StorageReference picRef = FirebaseUtils.mStorage.getReference().child(deal.getImageName());

                picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Log.d("Delete Image", "Image deleted successfully");

                    }
                }) .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Log.d("Delete Image", e.getMessage());

                    }
                });

            }

        }
    }

    private void backToList(){
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);

    }

    private void enableEditTexts (boolean isEnabled){

        txtTitle.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);

        if (FirebaseUtils.isAdmin){
            menu.findItem(R.id.save_menu).setVisible(true);
            menu.findItem(R.id.delete_menu).setVisible(true);
            enableEditTexts(true);

        }else {

            menu.findItem(R.id.save_menu).setVisible(false);
            menu.findItem(R.id.delete_menu).setVisible(false);
            enableEditTexts(false);

        }
        return true;
    }

    private void showImage(String url){

        if(url != null && url.isEmpty() == false){


            int width = Resources.getSystem().getDisplayMetrics().widthPixels;

            Picasso.get().load(url).resize(width, width*2/3).centerCrop().into(imageView);




        }
    }
}
