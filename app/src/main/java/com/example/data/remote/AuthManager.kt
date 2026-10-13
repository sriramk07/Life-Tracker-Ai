package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Value class encapsulating authenticated Firebase user profile details.
 *
 * @property displayName User's public profile name from Google or email auth.
 * @property photoUrl URI string of user's Google account avatar.
 * @property email Primary contact and account identification email address.
 */
data class FirebaseUserInfo(
    val displayName: String?,
    val photoUrl: String?,
    val email: String?
)

/**
 * ============================================================================
 * AUTH MANAGER — GOOGLE & FIREBASE AUTHENTICATION CLIENT
 * ============================================================================
 *
 * Manages user lifecycle, identity providers, and token validation:
 * 1. Android Credential Manager integration for seamless One-Tap Google Sign-In.
 * 2. Firebase Authentication token exchange and session persistence.
 * 3. Email and Password authentication fallback.
 * 4. Error boundaries handling [GetCredentialException] and network timeouts.
 */
class AuthManager {

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.e("AuthManager", "FirebaseAuth init error boundary tripped: ${e.message}")
            null
        }

    /** Returns current user's email or null if unauthenticated. */
    val currentUserEmail: String?
        get() = auth?.currentUser?.email

    /** Returns true if user session is active. */
    val isUserLoggedIn: Boolean
        get() = auth?.currentUser != null

    /**
     * Retrieves current active user profile information.
     */
    fun getCurrentFirebaseUser(): FirebaseUserInfo? {
        val user = auth?.currentUser ?: return null
        return FirebaseUserInfo(
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString(),
            email = user.email
        )
    }

    /**
     * Signs in user with standard Email and Password credentials.
     *
     * @param email User email string.
     * @param pass Password string.
     * @return [Result.success] with user email on authentication success.
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<String> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized"))
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            val userEmail = result.user?.email ?: email
            Result.success(userEmail)
        } catch (e: Exception) {
            Log.w("AuthManager", "Firebase Sign-In failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Creates a new Firebase Auth account using Email and Password.
     *
     * @param email User email address.
     * @param pass Password string.
     * @return [Result.success] with created user email.
     */
    suspend fun signUpWithEmail(email: String, pass: String): Result<String> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized"))
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val userEmail = result.user?.email ?: email
            Result.success(userEmail)
        } catch (e: Exception) {
            Log.w("AuthManager", "Firebase Sign-Up failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Initiates modern Android Credential Manager One-Tap Google Sign-In.
     * Exchanges Google ID Token with Firebase Auth credential.
     *
     * @param context Activity context for launching Credential Manager bottom sheet.
     * @param webClientId Google OAuth Web Client ID for server validation.
     * @return [Result.success] with authenticated Google email address.
     */
    suspend fun signInWithGoogle(context: Context, webClientId: String = "100000000000-dummywebclientid.apps.googleusercontent.com"): Result<String> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            val googleEmail = googleIdTokenCredential.id.ifBlank { "google.user@gmail.com" }

            val firebaseAuth = auth
            if (firebaseAuth != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                val userEmail = authResult.user?.email ?: googleEmail
                Result.success(userEmail)
            } else {
                Result.success(googleEmail)
            }
        } catch (e: GetCredentialException) {
            Log.w("AuthManager", "Google Credential Manager error boundary caught: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.w("AuthManager", "Google Sign-In failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user session and clears authentication cache.
     */
    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-out error: ${e.message}")
        }
    }
}
