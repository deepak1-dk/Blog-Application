package com.example.blogapp

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.service.autofill.UserData
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.blogapp.databinding.ActivitySignInAndRegistrationBinding
import com.example.blogapp.register.WelcomActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class SignInAndRegistrationActivity : AppCompatActivity()
{
private val binding : ActivitySignInAndRegistrationBinding by lazy {
    ActivitySignInAndRegistrationBinding.inflate(layoutInflater)
}
    private lateinit var auth : FirebaseAuth
    private lateinit var database : FirebaseDatabase
    private lateinit var storage : FirebaseStorage
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        // For visibility of fields
        val action = intent.getStringExtra("action")
        // Adjust visibility for login
        if (action == "login") {
            binding.loginEmailAddress.visibility = View.VISIBLE
            binding.loginPassword.visibility = View.VISIBLE
            binding.loginButton.visibility = View.VISIBLE

            binding.registerButton.isEnabled = false
            binding.registerButton.alpha = 0.5f
            binding.registerNewHere.isEnabled = false
            binding.registerNewHere.alpha = 0.5f

            binding.registerName.visibility = View.GONE
            binding.cardView.visibility = View.GONE
            binding.registerEmail.visibility = View.GONE
            binding.registerPassword.visibility = View.GONE

            binding.loginButton.setOnClickListener {
                val loginEmail : String = binding.loginEmailAddress.text.toString()
                val loginPassword : String = binding.loginPassword.text.toString()

                if (loginEmail.isEmpty() || loginPassword.isEmpty())
                {
                    Toast.makeText(this,"Please fill all the details",Toast.LENGTH_SHORT).show()
                }else
                {
                    auth.signInWithEmailAndPassword(loginEmail,loginPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful)
                            {
                                Toast.makeText(this,"Login Successful😊",Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this,MainActivity::class.java))
                                finish()
                                binding.loginEmailAddress.text.clear()
                                binding.loginPassword.text.clear()

                            }else{
                                Toast.makeText(this,"Login Failed: Please filled correct details",Toast.LENGTH_SHORT).show()

                            }
                        }
                }

            }

        }else if (action == "register") {
            binding.loginButton.isEnabled = false
            binding.loginButton.alpha = 0.5f

            binding.registerButton.setOnClickListener {
               // get data from edit text field
                val registerName : String = binding.registerName.text.toString()
                val registerEmail : String = binding.registerEmail.text.toString()
                val registerPassword : String = binding.registerPassword.text.toString()

                if (registerName.isEmpty() || registerEmail.isEmpty() || registerPassword.isEmpty()) {
                    Toast.makeText(this,"Please Fill All The Details",Toast.LENGTH_SHORT).show()
                } else {
                    auth.createUserWithEmailAndPassword(registerEmail, registerPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                               val user : FirebaseUser? = auth.currentUser
                                auth.signOut()
                                user?.let {
                                    // Save User Data into FireBase RealTime DataBase
                                    val userReference : DatabaseReference = database.getReference("users")
                                    val userId : String = user.uid
                                    val userData = com.example.blogapp.model.UserData(
                                        registerName,
                                        registerEmail
                                    )
                                    userReference.child(userId).setValue(userData)
                                        .addOnSuccessListener {
                                            Log.d("TAG","onCreate : data saved")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("TAG", "onCreate : Error saving data ${e.message}")

                                        }

                                    // Upload Image to FireBase Storage
                                    val storageReference = storage.reference.child("profile_image/$userId.jpg")
                                    storageReference.putFile(imageUri!!)

                                    Toast.makeText(this,"User Register Successfully", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this,WelcomActivity::class.java))
                                    finish()

                                }

                            }
                            else {
                                Toast.makeText(this,"User Register Failed", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            Log.d("TAG","failure exception : ${it.message}")
                        }
                }
            }
        }

        //setOnClickListener for the choose image
        binding.cardView.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Select Image"),
               PICK_IMAGE_REQUEST
            )
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            Glide.with(this)
                .load(imageUri)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.registerUserImage)
        }
    }
}