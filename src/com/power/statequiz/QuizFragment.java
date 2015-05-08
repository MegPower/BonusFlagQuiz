//QuizFragment.java
//contains the flag quiz logic
package com.power.statequiz;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;

public class QuizFragment extends Fragment 
{
	//String used when logging error message
	private static final String TAG = "StateQuiz Activity";
	
	private static final int STATES_IN_QUIZ = 10;
	
	//flag file name
	private List<String> fileNameList;
	//countries in current quiz
	private List<String> quizStatesList;
	//world regions in current quiz
	private Set<String> regionsSet;
	//correct country for the current flag
	private String correctAnswer;
	//number of guesses made
	private int totalGuesses;
	//number of correct guesses
	private int correctAnswers;
	//number of corrects on first try
	private int correctFirstTry = 0;
	//total correct first tries
	private int totalFirsts = 0;
	//total points 
	private int totalPoints;
	//top 5 scores
	private int s1;
	private int s2;
	private int s3;
	private int s4;
	private int s5;
	//number of rows displaying guess btns
	private int guessRows;
	//randomizes quiz
	private SecureRandom random;
	//delays loading next flag
	private Handler handler;
	//animates incorrect guess
	private Animation shakeAnimation;
	//shows current question
	private TextView questionNumberTextView;
	//displays a flag
	private ImageView flagImageView;
	//rows of answer btns
	private LinearLayout[] guessLinearLayouts;
	//displays correct! or incorrect!
	private TextView answerTextView;
	//correct cap
	private String correctCap;
	//submitted cap
	private String subCap;
	
	//configures QuizFragment when view is created
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_quiz, container, false);
		
		fileNameList = new ArrayList<String>();
		quizStatesList = new ArrayList<String>();
		random = new SecureRandom();
		handler = new Handler();
		
		//load shake animation for incorrect ans
		shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
		//repeat animation 3 times
		shakeAnimation.setRepeatCount(3);
		
		//get references to GUI components
		questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
		flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
		guessLinearLayouts = new LinearLayout[3];
		guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
		guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
		guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
		answerTextView = (TextView) view.findViewById(R.id.answerTextView);

		EditText editText1 = (EditText) view.findViewById(R.id.editText1);
		editText1.addTextChangedListener(editTextWatcher);
		
		//configure listeners for the guess btns
		for (LinearLayout row : guessLinearLayouts)
		{
			for (int column = 0; column < row.getChildCount(); column++)
			{
				Button button = (Button) row.getChildAt(column);
				button.setOnClickListener(guessButtonListener);
			}
		}
		
		//set questionNumberTextView's text
		questionNumberTextView.setText(getResources().getString(R.string.question, 1, STATES_IN_QUIZ));
		//return fragment's view for display
		return view;
	}//end onCreateView
	
	//update guessRows based on value in SharedPreferences
	public void updateGuessRows(SharedPreferences sharedPreferences)
	{
		//get the number of guess btns to be displayed
		String choices = sharedPreferences.getString(MainActivity.CHOICES, null);
		guessRows = Integer.parseInt(choices) / 3;
		
		//hide all guess btns linearlayouts
		for (LinearLayout layout : guessLinearLayouts)
			layout.setVisibility(View.INVISIBLE);
		
		//display appropriate guess btn linearlayouts
		for (int row = 0; row < guessRows; row++)
			guessLinearLayouts[row].setVisibility(View.VISIBLE);
	}//end updateGuessRows
	
	//update world regions for quiz based on sharedpreferences
	public void updateRegions(SharedPreferences sharedPreferences)
	{
		regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
	}
	
	//set up and start quiz
	public void resetQuiz()
	{
		//use AssetManager to get image file for enabled regions
		AssetManager assets = getActivity().getAssets();
		//empty list
		fileNameList.clear();
		
		try
		{
			//loop through each region
			for (String region : regionsSet)
			{
				String[] paths = assets.list(region);
			
				for (String path : paths)
					fileNameList.add(path.replace(".jpg", ""));
			};
		
		}
		catch (IOException exception)
		{
			Log.e(TAG, "Error Loading image file names", exception);
		}
		
		//reset the number of correct answers made
		correctAnswers = 0;
		//reset the total number of guesses the user made
		totalGuesses = 0;
		//clear totalPoints
		totalPoints = 0;
		//clear totalFirsts
		totalFirsts = 0;
		//clear prior list of quiz countries
		quizStatesList.clear();
		
		int flagCounter = 1;
		int numberOfFlags = fileNameList.size();
		
		//add STATES_in_Quiz random file names to the quizStatesList
		while (flagCounter <= STATES_IN_QUIZ)
		{
			int randomIndex = random.nextInt(numberOfFlags);
			
			//get random file name
			String fileName = fileNameList.get(randomIndex);
			
			//if region is enabled and hasn't been chosen
			if (!quizStatesList.contains(fileName))
			{
				//add file to the list
				quizStatesList.add(fileName);
				++flagCounter;
			}
		}
		
		//start quiz by loading first flag
		loadNextState();
	}//end resetQuiz

	// after the user guesses a correct flag, load the next flag
	private void loadNextState() 
	{
	   // get file name of the next flag and remove it from the list
	   String nextImage = quizStatesList.remove(0);
	   correctAnswer = nextImage; // update the correct answer
	   answerTextView.setText(""); // clear answerTextView 

	   // display current question number
	   questionNumberTextView.setText(
	      getResources().getString(R.string.question, 
	         (correctAnswers + 1), STATES_IN_QUIZ));
	   
	   String region = nextImage.substring(0, nextImage.indexOf('-'));
	   String capital = nextImage.substring(nextImage.indexOf('+'), nextImage.indexOf('.'));

	   // use AssetManager to load next image from assets folder
	   AssetManager assets = getActivity().getAssets(); 

	   try
	   {
	      // get an InputStream to the asset representing the next flag
	      InputStream stream = 
	         assets.open(region + "/" + nextImage + "+" + capital + ".jpg");
	      
	      // load the asset as a Drawable and display on the flagImageView
	      Drawable flag = Drawable.createFromStream(stream, nextImage);
	      flagImageView.setImageDrawable(flag);                       
	   } 
	   catch (IOException exception)  
	   {
	      Log.e(TAG, "Error loading " + nextImage, exception);
	   } 

	   Collections.shuffle(fileNameList); // shuffle file names

	   // put the correct answer at the end of fileNameList
	   int correct = fileNameList.indexOf(correctAnswer);
	   fileNameList.add(fileNameList.remove(correct));

	   // add 3, 6, or 9 guess Buttons based on the value of guessRows
	   for (int row = 0; row < guessRows; row++) 
	   {
	      // place Buttons in currentTableRow
	      for (int column = 0; 
	         column < guessLinearLayouts[row].getChildCount(); column++) 
	      { 
	         // get reference to Button to configure
	         Button newGuessButton = 
	            (Button) guessLinearLayouts[row].getChildAt(column);
	         newGuessButton.setEnabled(true);

	         // get country name and set it as newGuessButton's text
	         String fileName = fileNameList.get((row * 3) + column);
	         newGuessButton.setText(getStateName(fileName));
	      } 
	   } 
	   
	   // randomly replace one Button with the correct answer
	   int row = random.nextInt(guessRows); // pick random row
	   int column = random.nextInt(3); // pick random column
	   LinearLayout randomRow = guessLinearLayouts[row]; // get the row
	   String stateName = getStateName(correctAnswer);
	   ((Button) randomRow.getChildAt(column)).setText(stateName);    
	} // end method loadNextFlag
	
	//parses the country flag file name and returns country name
	private String getStateName(String name)
	{
		String cleanName = name.substring(name.indexOf('-') + 1).replace('-', ' ');
		return cleanName;
	}
	
	private String getStateCap(String name)
	{
		String cleanName = name.substring(name.indexOf('+') + 1).replace('-', ' ');
		return cleanName;
	}
	
	private TextWatcher editTextWatcher = new TextWatcher()
	{
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
			subCap = s.toString();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {
		}
	};

	
	//called when a guess btn is touched
	private OnClickListener guessButtonListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			Button guessButton = ((Button) v);
			String guess = guessButton.getText().toString();
			String answer = getStateName(correctAnswer);
			//increment number of guesses the user has made
			++totalGuesses;
			correctFirstTry++;
						
			//if answer is correct
			if(guess.equals(answer))
			{
				//if answer is correct on first try
				if(correctFirstTry == 1)
				{
					totalFirsts++;
				}
				//increment the number of correct answers
				++correctAnswers;
				correctFirstTry = 0;
				
				//subtract from total score if not a first guess
				totalPoints = (STATES_IN_QUIZ * guessRows * 3) - totalGuesses;
				
				//display correct answer in green text
				answerTextView.setText(answer + "!");
				answerTextView.setTextColor(getResources().getColor(R.color.correct_answer));
				
				//turn off all buttons
				disableButtons();
				
				//ask about capital 
				//make layout visible
				guessLinearLayouts[3].setVisibility(View.VISIBLE);
				correctCap = getStateCap(correctAnswer);
				if(subCap.equals(correctCap))
					totalPoints = totalPoints + 10;
				
				//if user has correctly identified STATES_IN_QUIZ flags
				if (correctAnswers == STATES_IN_QUIZ)
				{
					//update top scores
					if (totalPoints > s1)
					{
						s5 = s4;
						s4 = s3;
						s3 = s2;
						s2 = s1;
						s1 = totalPoints;
					}
					else if (totalPoints > s2)
					{
						s5 = s4;
						s4 = s3;
						s3 = s2;
						s2 = totalPoints;
					}
					else if (totalPoints > s3)
					{
						s5 = s4;
						s4 = s3;
						s3 = totalPoints;
					}
					else if (totalPoints > s4)
					{
						s5 = s4;
						s4 = totalPoints;
					}
					else if (totalPoints > s5)
					{
						s5 = totalPoints;
					}
					
					//dialogfragment to display quiz stats and start new quiz
					DialogFragment quizResults = new DialogFragment()
					{
						//create alertDialog and return it
						@Override
						public Dialog onCreateDialog(Bundle bundle)
						{
							AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
							builder.setCancelable(false);
							
							//display results
							builder.setMessage(getResources().getString(R.string.results, totalGuesses, 
									(1000 / (double) totalGuesses), totalFirsts, totalPoints, s1, s2, s3, s4, s5));

							//reset quiz button
							builder.setPositiveButton(R.string.reset_quiz, 
									new DialogInterface.OnClickListener()
									{
										public void onClick(DialogInterface dialog, int id) 
										{
											resetQuiz();
										}
									} 
									);
								//return the AlertDialog
							return builder.create();
							
						}// end dialog onCreateDialog
					};//end dialogFragment
					
					//use FragmentMember to display the DialogFragment
					quizResults.show(getFragmentManager(), "quiz results");
				}
				//answer is correct but quiz is not over
				else
				{
					//load the next flag after a 1-second delay
					handler.postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							loadNextState();
						}
					}, 2000);
				}
			}
			//guess was incorrect
			else
			{
				//play shake
				flagImageView.startAnimation(shakeAnimation);
				
				//display incorrect in red
				answerTextView.setText(R.string.incorrect_answer);
				answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));
				//disable incorrect answer
				guessButton.setEnabled(false);
			}
		}
	};//end guessButtonListener
	
	//utility method that disables all answer buttons
	private void disableButtons()
	{
		for (int row = 0; row < guessRows; row++)
		{
			LinearLayout guessRow = guessLinearLayouts[row];
			for (int i = 0; i < guessRow.getChildCount(); i++)
				guessRow.getChildAt(i).setEnabled(false);
		}
	}
}//end flagquiz class