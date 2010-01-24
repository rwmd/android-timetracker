package net.ser1.timetracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class EditTask extends Activity implements OnClickListener {

    public static final String TASK = "Task";
    public static final String TAGS = "Tags";
    private Task task;
    private Set<Tag> tags;
    private List<Spinner> tagViews;
    private ArrayAdapter<Tag> tagsModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.preferences);
        Bundle extras = getIntent().getExtras();
        task = (Task)extras.getSerializable(TASK);
        Set<Tag> tags = (Set<Tag>)extras.getSerializable(TAGS);
        if (task == null | tags == null) {
            setResult(Activity.RESULT_CANCELED, getIntent());
            finish();
        }
        tagViews = new ArrayList<Spinner>();
        LayoutInflater factory = LayoutInflater.from(this);
        factory.inflate(R.layout.edit_task, null);
        tagsModel = new ArrayAdapter<Tag>(this, android.R.layout.simple_spinner_item);
        tagsModel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (Tag t : tags) {
            tagsModel.add(t);
        }
        Spinner tagView = (Spinner)findViewById(R.id.tag_chooser);
        tagViews.add(tagView);
        tagView.setAdapter(tagsModel);
        ImageButton addTag = (ImageButton)findViewById(R.id.add_tag);
        addTag.setOnClickListener(this);
        super.onCreate(savedInstanceState);
    }
    
    private Spinner makeSpinner(Tag newTag) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(tagsModel);
        LinearLayout spinners = (LinearLayout) findViewById(R.id.tag_spinners);
        spinners.addView(spinner, spinners.getChildCount());
        tagViews.add(spinner);
        spinner.setSelection(tagsModel.getPosition(newTag));
        return spinner;
    }
    
    private void recordAndReturn() {
        EditText text = (EditText) findViewById(R.id.task_edit_name_edit);
        task.setTaskName(text.getText().toString());
        for (Spinner tagView : tagViews) {
            Tag tag = tagsModel.getItem(tagView.getSelectedItemPosition());
            tag.addTask(task);
        }
        Intent intent = new Intent(this, EditTask.class);
        intent.putExtra(TASK, task);
        // TODO: Sets can't be serialized
        //intent.putExtra(TAGS, tags);
        setResult(Activity.RESULT_OK, getIntent());
        finish();
    }
    
    @Override
    protected void onPause() {
        System.err.println("onPause");
        
        super.onPause();
    }
    
    @Override
    protected void onStop() {
        recordAndReturn();
        super.onStop();
    }

    public void onClick(View v) {
        String name = findViewById(R.id.new_tag_edit).toString();
        Tag newTag = new Tag(name);
        if (!tags.contains(newTag)) {
            tags.add(newTag);
            tagsModel.add(newTag);
            makeSpinner( newTag );
        }
    }
}