package com.example.stepcounter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

const val TAG = "StepCounter"

class MainActivity : AppCompatActivity() {

    // Set all values to be shared throughout app
    private val dateFormat = DateFormat.getDateInstance()
    private val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    private val currentDate = Date()
    private var begRange = currentDate.time
    private var endRange = currentDate.time

    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()
    }

    // temp data until updated.
    private var pastWeekSteps = arrayOf<Int>(100, 200, 400, 800, 1600, 3200, 6400)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Display "not signed in" until sign in completed successfully.

        val usernameText = findViewById<TextView>(R.id.user_signed_in_text)
        usernameText.text = "Not signed in"

        // Setup the Google Sign In button

        val signInButton = findViewById<SignInButton>(R.id.sign_in_button)
        signInButton.setSize(SignInButton.SIZE_STANDARD)

        signInButton.setOnClickListener {
            signIn()
        }

        // Set initial time
        calendar.time = currentDate

        // Get beginning of week
        val yr = calendar.get(Calendar.YEAR)
        val mo = calendar.get(Calendar.MONTH)
        val days = calendar.get(Calendar.DAY_OF_MONTH) - calendar.get(Calendar.DAY_OF_WEEK) + 8
        calendar.set(yr, mo, days)

        // Set to midnight
        calendar.set(Calendar.HOUR, 7)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.AM_PM, 0)
    }

    private fun signIn() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        val signInIntent: Intent = GoogleSignIn.getClient(this, gso).signInIntent

        // Technically deprecated but still works...
        startActivityForResult(signInIntent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == 1) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
            handleSuccessfulSignIn(account)

        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
        }
    }

    override fun onStart() {
        super.onStart()

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            handleSuccessfulSignIn(account)
        } else {
            // Do nothing...
        }
    }

    private fun handleSuccessfulSignIn(account: GoogleSignInAccount) {
        updateUI(account)
        val signInButton = findViewById<SignInButton>(R.id.sign_in_button)
        signInButton.visibility = View.GONE
        readHistoryData()

        // Print name to log
        Log.e(TAG, "Google signed in under ${account.displayName}\n")
    }

    private fun updateUI(account: GoogleSignInAccount) {

        // Update username being displayed

        val usernameText = findViewById<TextView>(R.id.user_signed_in_text)
        usernameText.text = "Signed in as ${account.displayName}"

        // Change steps displayed

        val weeklyStepsText = findViewById<TextView>(R.id.weekly_text)
        val weeklyStepsNum = findViewById<TextView>(R.id.weekly_num)

        val weekRange = findViewById<TextView>(R.id.date_above_chart)
        val fmtOut = SimpleDateFormat("MMM dd")
        weekRange.text = "${fmtOut.format(begRange)} - ${fmtOut.format(endRange)}"

        // Get all component ID's

        val sundayData = findViewById<TextView>(R.id.sunday_data)
        val sundayText = findViewById<TextView>(R.id.sunday_text)

        val mondayData = findViewById<TextView>(R.id.monday_data)
        val mondayText = findViewById<TextView>(R.id.monday_text)

        val tuesdayData = findViewById<TextView>(R.id.tuesday_data)
        val tuesdayText = findViewById<TextView>(R.id.tuesday_text)

        val wednesdayData = findViewById<TextView>(R.id.wednesday_data)
        val wednesdayText = findViewById<TextView>(R.id.wednesday_text)

        val thursdayData = findViewById<TextView>(R.id.thursday_data)
        val thursdayText = findViewById<TextView>(R.id.thursday_text)

        val fridayData = findViewById<TextView>(R.id.friday_data)
        val fridayText = findViewById<TextView>(R.id.friday_text_text)

        val saturdayData = findViewById<TextView>(R.id.saturday_data)
        val saturdayText = findViewById<TextView>(R.id.saturday_text)

        // Calculate avg steps from the week

        weeklyStepsText.text = "Weekly Average"
        weeklyStepsNum.text = "${pastWeekSteps.average().toInt()}"

        // Set chart labels -- technically doesn't need to do all of these each time... we don't
        // currently need to save the processing speed so we'll leave it.

        sundayData.text = "${pastWeekSteps[0]}"
        sundayText.text = "Sun"

        mondayData.text = "${pastWeekSteps[1]}"
        mondayText.text = "Mon"

        tuesdayData.text = "${pastWeekSteps[2]}"
        tuesdayText.text = "Tue"

        wednesdayData.text = "${pastWeekSteps[3]}"
        wednesdayText.text = "Wed"

        thursdayData.text = "${pastWeekSteps[4]}"
        thursdayText.text = "Thu"

        fridayData.text = "${pastWeekSteps[5]}"
        fridayText.text = "Fri"

        saturdayData.text = "${pastWeekSteps[6]}"
        saturdayText.text = "Sat"
    }

    /**
     * Gets a Google account -- or uses last signed in account,
     */
    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)


    /**
     * Asynchronous task to read the history data. When the task succeeds, it will print out and
     * record the data. At the final stage, updateUI is called, updating the user screen with the
     * new values.
     */
    private fun readHistoryData(relativeWeek: Int = +1): Task<DataReadResponse> {
        // Begin by creating the query (get the DataReadResponse task).
        val readRequest = queryFitnessData(relativeWeek)

        // Invoke the History API to fetch the data with the query
        return Fitness.getHistoryClient(this, getGoogleAccount())
            .readData(readRequest)
            .addOnSuccessListener { dataReadResponse ->
                // Print / record data.
                printData(dataReadResponse)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "There was a problem reading the data.", e)
            }
    }

    /** Returns a [DataReadRequest] for all step count changes in the past week.  */
    private fun queryFitnessData(relativeWeek: Int = +1): DataReadRequest {
        // Setting a start and end date using a range of 1 week before this moment.
        calendar.time = currentDate

        // Get beginning of week
        val yr = calendar.get(Calendar.YEAR)
        val mo = calendar.get(Calendar.MONTH)
        val days = calendar.get(Calendar.DAY_OF_MONTH) - calendar.get(Calendar.DAY_OF_WEEK) + 1
        calendar.set(yr, mo, days)

        // Set to midnight
        calendar.set(Calendar.HOUR, 7)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.AM_PM, 0)

        begRange = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, relativeWeek) // Ready to add extra functionality (see previous weeks). Checks current week by default
        endRange = calendar.timeInMillis

        Log.i(TAG, "Range Start: ${DateFormat.getDateTimeInstance().format(begRange)}")
        Log.i(TAG, "Range End: ${DateFormat.getDateTimeInstance().format(endRange)}")

        return DataReadRequest.Builder()
            // The double-parameter .aggregate function call is deprecated -- only the AGGREGATE_STEP_COUNT_DELTA is necessary now.
            .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
            // bucketByTime allows for a time span (1 24-hour day)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(begRange, endRange, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * Parse through data, print to log (unprofessional, but helpful!), and update UI array.
     */
    private fun printData(dataReadResult: DataReadResponse) {
        // https://developers.google.com/android/reference/com/google/android/gms/fitness/HistoryClient.html#public-taskdatareadresponse-readdata-datareadrequest-request
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if (dataReadResult.buckets.isNotEmpty()) {
            Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)

            var i = 0
            for (bucket in dataReadResult.buckets) {
                    bucket.dataSets.forEach() { dumpDataSet(it, i)}
                i += 1
            }
            updateUI(getGoogleAccount())
        }
    }

    /**
     * Further parsing from printData function
     */
    private fun dumpDataSet(dataSet: DataSet, day: Int) {
        Log.i(TAG, "Data returned for Data type ${dataSet.dataType.name}")

        for (dp in dataSet.dataPoints) {
            Log.i(TAG, " - Data point:")
            Log.i(TAG, "\t\tType: ${dp.dataType.name}")
            Log.i(TAG, "\t\tStart: ${dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))}")
            Log.i(TAG, "\t\tEnd: ${dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))}")
            dp.dataType.fields.forEach {
                Log.i(TAG, "\t\t\tField: ${it.name}")
                Log.i(TAG, "\t\t\tValue: ${dp.getValue(it)}")
                pastWeekSteps[day] = "${dp.getValue(it)}".toInt()
            }
        }

        // If null (future date) set steps to zero.
        if (dataSet.dataPoints.isEmpty()) {
            pastWeekSteps[day] = 0
            Log.i(TAG, "\t\tEmpty data set")
        }
    }





}