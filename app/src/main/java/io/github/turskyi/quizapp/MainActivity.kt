package io.github.turskyi.quizapp

import android.database.Cursor
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.turskyi.droidtermsprovider.DroidTermsExampleContract
import java.lang.ref.WeakReference

/**
 * Gets the data from the ContentProvider  of the app from here
 * https://github.com/udacity/DroidTermsExample-APK/raw/master/droidtermsexample-release.apk
 * and shows a series of flash cards.
 */
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    companion object {
        /* This state is when the word definition is hidden and clicking the button will therefore
         show the definition */
        private const val STATE_HIDDEN = 0

        /* This state is when the word definition is shown and clicking the button will therefore
         advance the app to the next word */
        private const val STATE_SHOWN = 1
    }
    /* The data from the DroidTermsExample content provider */
    private var mData: Cursor? = null

    /* The current state of the app */
    private var mCurrentState = 0

    /* The index of the definition and word column in the cursor */
    private var mDefColumn = 0
    private var mWordColumn = 0
    private var mWordTextView: TextView? = null
    private var mDefinitionTextView: TextView? = null
    private var mButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()

        /* Run the database operation to get the cursor off of the main thread */
        WordFetchTask(this).execute()
    }

    private fun initView() {
        mWordTextView = findViewById(R.id.text_view_word)
        mDefinitionTextView = findViewById(R.id.text_view_definition)
        mButton = findViewById(R.id.button_next)
    }

    /**
     * This is called from the layout when the button is clicked and switches between the
     * two app states.
     * @param @onButtonClick The view that was clicked
     */
    @Suppress("unused")
    fun View?.onButtonClick() {

        /* Either show the definition of the current word, or if the definition is currently
         showing, move to the next word. */
        when (mCurrentState) {
            STATE_HIDDEN -> showDefinition()
            STATE_SHOWN -> nextWord()
        }
    }

    fun nextWord() {
        /* If you reach the end of the list of words, you should start at the beginning again. */
        mData?.let{
            /* Move to the next position in the cursor, if there isn't one, move to the first */
            if (mData?.moveToNext() == false) {
                mData?.moveToFirst()
            }
            /* Hide the definition TextView */
            mDefinitionTextView?.visibility = View.INVISIBLE

            /* Change button text */
            mButton?.text = getString(R.string.show_definition)

            /* Get the next word */
            mWordTextView?.text = mData?.getString(mWordColumn)
            mDefinitionTextView?.text = mData?.getString(mDefColumn)
            mCurrentState = STATE_HIDDEN
        }
    }

    private fun showDefinition() {
       mData?.let{
            /* Show the definition TextView */
            mDefinitionTextView?.visibility = View.VISIBLE

            /* Change button text */
            mButton?.text = getString(R.string.next_word)
            mCurrentState = STATE_SHOWN
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mData?.close()
    }

    /* an async task to do the data fetch off of the main thread. */
    private class WordFetchTask(context: MainActivity): AsyncTask<Void?, Void?, Cursor?>() {

        private val activityReference: WeakReference<MainActivity> = WeakReference(context)
        /* get a reference to the activity if it is still there */
        val activity = activityReference.get()
        /* Invoked on a background thread */
        override fun doInBackground(vararg params: Void?): Cursor? {
            /* Make the query to get the data */



            /* Get the content resolver */
            val resolver = activity?.contentResolver

            /* Call the query method on the resolver with the correct Uri from the contract class */
            return resolver?.query(
                DroidTermsExampleContract.CONTENT_URI,
                null, null, null, null
            )
        }

        /* Invoked on UI thread */
        override fun onPostExecute(cursor: Cursor?) {
            super.onPostExecute(cursor)

            /* Set the data for MainActivity */
            activity?.mData = cursor
            /* Get the column index, in the Cursor, of each piece of data */
            activity?.mData?.getColumnIndex(DroidTermsExampleContract.COLUMN_DEFINITION)?.let {
                activity.mDefColumn = it
            }

            activity?.mData?.getColumnIndex(DroidTermsExampleContract.COLUMN_WORD)?.let {
                activity.mWordColumn = it
            }

            /* Set the initial state */
            activity?.nextWord()
        }
    }
}