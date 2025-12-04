package org.basnalcorp.indiecomicapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import org.basnalcorp.indiecomicapp.R
import org.basnalcorp.indiecomicapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    private companion object {
        const val RC_SIGN_IN = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        // Get the default web client ID from Firebase-generated resources
        val webClientId = try {
            getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            // If web client ID is not found, show error and return
            Toast.makeText(requireContext(), "Web client ID not found. Please configure Firebase properly.", Toast.LENGTH_LONG).show()
            return root
        }
        
        if (webClientId.isEmpty()) {
            Toast.makeText(requireContext(), "Web client ID is empty. Please configure Firebase properly.", Toast.LENGTH_LONG).show()
            return root
        }
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Observe greeting text changes
        homeViewModel.greetingText.observe(viewLifecycleOwner) {
            binding.textHome.text = it
        }

        // Set up sign-in button click listener
        binding.btnSignIn.setOnClickListener {
            signIn()
        }

        // Check if user is already signed in
        checkCurrentUser()

        return root
    }

    private fun checkCurrentUser() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // User is signed in
            updateUI(currentUser.displayName)
            binding.btnSignIn.visibility = View.GONE
        } else {
            // User is not signed in
            binding.btnSignIn.visibility = View.VISIBLE
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign-In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign-In failed
                val errorMessage = when (e.statusCode) {
                    10 -> "DEVELOPER_ERROR: Please add SHA-1 fingerprint to Firebase Console. Error code: ${e.statusCode}"
                    12501 -> "Sign-in cancelled by user"
                    else -> "Sign-in failed: ${e.message} (Error code: ${e.statusCode})"
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        if (account == null) return

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign-in success
                    val user = firebaseAuth.currentUser
                    updateUI(user?.displayName)
                    binding.btnSignIn.visibility = View.GONE
                    Toast.makeText(requireContext(), "Signed in successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    // Sign-in failed
                    Toast.makeText(requireContext(), "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUI(userName: String?) {
        homeViewModel.updateGreeting(userName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}