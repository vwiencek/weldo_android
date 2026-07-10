package com.fginc.weldo.data.remote

import com.fginc.weldo.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The full backend surface (see ../doit_back/JSON.md). Suspend functions returning the
 * body throw HttpException on non-2xx; endpoints where a specific status is meaningful
 * (401/404/409) return Response<T> so callers can branch.
 */
interface WeldoApi {

    // ---- Auth ----
    @POST("auth/apple")
    suspend fun loginApple(@Body body: AppleLoginRequest): AuthTokenResponse

    @POST("auth/password/login")
    suspend fun passwordLogin(@Body body: PasswordAuthRequest): Response<AuthTokenResponse>

    @POST("auth/password/register")
    suspend fun passwordRegister(@Body body: PasswordAuthRequest): Response<AuthTokenResponse>

    // ---- Aggregate ----
    @GET("items")
    suspend fun getItems(): AllItems

    @POST("items/batch")
    suspend fun createBatch(@Body body: BatchCreateRequest): BatchCreateResponse

    // ---- Task (singular path) ----
    @POST("task")
    suspend fun createTask(@Body body: Task): Task

    @GET("task/{id}")
    suspend fun getTask(@Path("id") id: String): Task

    @PUT("task/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body body: Task): Task

    @DELETE("task/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Unit>

    @PUT("task/{id}/completed")
    suspend fun setTaskCompleted(@Path("id") id: String, @Body body: CompletionUpdate): Task

    // ---- Project ----
    @POST("projects")
    suspend fun createProject(@Body body: Project): Project

    @GET("projects/{id}")
    suspend fun getProject(@Path("id") id: String): Project

    @PUT("projects/{id}")
    suspend fun updateProject(@Path("id") id: String, @Body body: Project): Project

    @DELETE("projects/{id}")
    suspend fun deleteProject(@Path("id") id: String): Response<Unit>

    @PUT("projects/{id}/completed")
    suspend fun setProjectCompleted(@Path("id") id: String, @Body body: CompletionUpdate): Project

    @GET("projects/{id}/items")
    suspend fun getProjectItems(@Path("id") id: String): ProjectItems

    // ---- Commitment ----
    @POST("commitments")
    suspend fun createCommitment(@Body body: Commitment): Commitment

    @GET("commitments/{id}")
    suspend fun getCommitment(@Path("id") id: String): Commitment

    @PUT("commitments/{id}")
    suspend fun updateCommitment(@Path("id") id: String, @Body body: Commitment): Commitment

    @DELETE("commitments/{id}")
    suspend fun deleteCommitment(@Path("id") id: String): Response<Unit>

    @PUT("commitments/{id}/completed")
    suspend fun setCommitmentCompleted(@Path("id") id: String, @Body body: CompletionUpdate): Commitment

    // ---- Reminder ----
    @POST("reminders")
    suspend fun createReminder(@Body body: Reminder): Reminder

    @GET("reminders/{id}")
    suspend fun getReminder(@Path("id") id: String): Reminder

    @PUT("reminders/{id}")
    suspend fun updateReminder(@Path("id") id: String, @Body body: Reminder): Reminder

    @DELETE("reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: String): Response<Unit>

    @PUT("reminders/{id}/completed")
    suspend fun setReminderCompleted(@Path("id") id: String, @Body body: CompletionUpdate): Reminder

    // ---- WaitingFor ----
    @POST("waiting-for")
    suspend fun createWaitingFor(@Body body: WaitingFor): WaitingFor

    @GET("waiting-for/{id}")
    suspend fun getWaitingFor(@Path("id") id: String): WaitingFor

    @PUT("waiting-for/{id}")
    suspend fun updateWaitingFor(@Path("id") id: String, @Body body: WaitingFor): WaitingFor

    @DELETE("waiting-for/{id}")
    suspend fun deleteWaitingFor(@Path("id") id: String): Response<Unit>

    @PUT("waiting-for/{id}/completed")
    suspend fun setWaitingForCompleted(@Path("id") id: String, @Body body: CompletionUpdate): WaitingFor

    // ---- Idea ----
    @POST("ideas")
    suspend fun createIdea(@Body body: Idea): Idea

    @GET("ideas/{id}")
    suspend fun getIdea(@Path("id") id: String): Idea

    @PUT("ideas/{id}")
    suspend fun updateIdea(@Path("id") id: String, @Body body: Idea): Idea

    @DELETE("ideas/{id}")
    suspend fun deleteIdea(@Path("id") id: String): Response<Unit>

    // ---- Routine ----
    @POST("routines")
    suspend fun createRoutine(@Body body: Routine): Routine

    @GET("routines/{id}")
    suspend fun getRoutine(@Path("id") id: String): Routine

    @PUT("routines/{id}")
    suspend fun updateRoutine(@Path("id") id: String, @Body body: Routine): Routine

    @DELETE("routines/{id}")
    suspend fun deleteRoutine(@Path("id") id: String): Response<Unit>

    // ---- Suggestion ----
    @POST("suggestions")
    suspend fun createSuggestion(@Body body: Suggestion): Suggestion

    @GET("suggestions/{id}")
    suspend fun getSuggestion(@Path("id") id: String): Suggestion

    @PUT("suggestions/{id}")
    suspend fun updateSuggestion(@Path("id") id: String, @Body body: Suggestion): Suggestion

    @DELETE("suggestions/{id}")
    suspend fun deleteSuggestion(@Path("id") id: String): Response<Unit>

    // ---- Note ----
    @POST("notes")
    suspend fun createNote(@Body body: Note): Note

    @GET("notes/{id}")
    suspend fun getNote(@Path("id") id: String): Note

    @PUT("notes/{id}")
    suspend fun updateNote(@Path("id") id: String, @Body body: Note): Note

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): Response<Unit>

    // ---- Capture ----
    @POST("capture")
    suspend fun capture(
        @Body body: CaptureRequest,
        @Header("X-Timezone") tz: String,
    ): CaptureProposal

    @POST("capture/image")
    suspend fun captureImage(
        @Body body: CaptureImageRequest,
        @Header("X-Timezone") tz: String,
    ): CaptureProposal

    // ---- Profile & preferences ----
    @GET("profile")
    suspend fun getProfile(): UserProfile

    @PUT("profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): Response<UserProfile>

    @GET("profile/handle-check")
    suspend fun handleCheck(@Query("handle") handle: String): Response<HandleCheckResponse>

    @PUT("profile/preferences/{key}")
    suspend fun putPreference(@Path("key") key: String, @Body body: PreferenceValue): Response<Unit>

    // ---- Statistics ----
    @GET("statistics")
    suspend fun getStatistics(
        @Query("period") period: String,
        @Header("X-Timezone") tz: String,
    ): Statistics

    // ---- Attachments ----
    @POST("attachments")
    suspend fun createAttachment(@Body body: AttachmentCreateRequest): AttachmentMeta

    @GET("attachments")
    suspend fun listAttachments(
        @Query("itemType") itemType: String,
        @Query("itemId") itemId: String,
    ): List<AttachmentMeta>

    @GET("attachments/{id}")
    suspend fun getAttachment(@Path("id") id: String): AttachmentData

    @DELETE("attachments/{id}")
    suspend fun deleteAttachment(@Path("id") id: String): Response<Unit>
}
