package com.itaisapir.yum;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.itaisapir.yum.Logic.CategoryOrRecipe;
import com.itaisapir.yum.Logic.CategoryRecipeAdapter;
import com.itaisapir.yum.Logic.Recipe;
import com.itaisapir.yum.utils.InnerIds;
import com.itaisapir.yum.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldCategoryFragment  extends Fragment implements CategoryRecipeAdapter.OnItemClicked{
    OnWorldCategoryActionListener worldListener;

    private Utils utils;
    private DatabaseReference categoryRef;
    private TextView worldTitle;
    private EditText searchField;

    private RecyclerView categoryRecipeRecyclerView;
    private CategoryRecipeAdapter categoryRecipeRecyclerAdapter;
    private RecyclerView.LayoutManager categoryRecipeLayoutManager;

    private int level; // level 0  sub category and recipes, level 1 recipes only

    private final String basePath = "";
    private String currentInnerItemsPath;
    private String currentUsersTokenPath;

    private String searchFirstLevel;
    private String token;

    private List<CategoryOrRecipe> categoryAndRecipeList;
    private List<CategoryOrRecipe> searchCategoryAndRecipeList;

    private ValueEventListener recyclerListener;
    private TextWatcher searchListener;
    private Button subscribeButton;
    public int currentCategoryIndex;
    private LinearLayout listOfCategories;
    private LinearLayout searchBarIsEmpty;
    private TextView emptyOrNotFoundText;

    public WorldCategoryFragment(){}
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            worldListener = (OnWorldCategoryActionListener) context;
        } catch(ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnWorldCategoryActionListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_world_category, container, false);
        worldTitle = view.findViewById(R.id.worldTitle);
        worldListener.setCurrentPath(currentInnerItemsPath);
        searchBarIsEmpty = view.findViewById(R.id.searchBarIsEmpty);
        emptyOrNotFoundText = view.findViewById(R.id.emptyOrNotFoundText);
        listOfCategories = view.findViewById(R.id.listOfCategories);
        categoryAndRecipeList = new ArrayList<>();
        utils = Utils.getInstance();
        categoryRef = FirebaseDatabase.getInstance().getReference(InnerIds.PUBLIC);
        searchCategoryAndRecipeList = new ArrayList<>();

        currentCategoryIndex = 0;
        initToken();

        subscribeButton = view.findViewById(R.id.subscribeButton);
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(token == null)
                    initToken();
                if (token!=null) {
                    if(subscribeButton.getText().equals(getResources().getString(R.string.subscribe)))
                    {
                        Map<String, Object> tokens = new HashMap<>();
                        tokens.put(token, token);
                        categoryRef.child(currentUsersTokenPath).updateChildren(tokens);
                        Toast toast = utils.createBigToast(getContext(), getResources().getString(R.string.subscribe_success), Toast.LENGTH_SHORT);
                        if(toast != null )
                            toast.show();
                        subscribeButton.setText(getResources().getString(R.string.unsubscribe));
                    }else{
                        categoryRef.child(currentUsersTokenPath).child(token).removeValue();
                        Toast toast = utils.createBigToast(getContext(), getResources().getString(R.string.unsubscribe_success), Toast.LENGTH_SHORT);
                        if(toast != null )
                            toast.show();
                        subscribeButton.setText(getResources().getString(R.string.subscribe));
                    }
                } else {
                    Toast toast = utils.createBigToast(getContext(), getResources().getString(R.string.subscribe_fail), Toast.LENGTH_SHORT);
                    if (toast != null)
                        toast.show();
                }
            }
        });

        level = 0;
        searchField = view.findViewById(R.id.searchField);

        categoryRecipeRecyclerView = view.findViewById(R.id.categoryRecipeList);
        worldListener.setRecyclerView(categoryRecipeRecyclerView);

        categoryRecipeRecyclerView.setHasFixedSize(true);
        categoryRecipeLayoutManager = new LinearLayoutManager(getContext());
        categoryRecipeRecyclerView.setLayoutManager(categoryRecipeLayoutManager);

        currentInnerItemsPath = basePath;
        createListenerRecycler();
        createSearchListener();
        enterCategory(currentInnerItemsPath);

        searchField.addTextChangedListener(searchListener);
        worldListener.setFragment(this);
        return view;
    }

    private void initToken() {
        // Get token
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            token = null;
                            return;
                        }
                        // Get new Instance ID token
                        token = task.getResult().getToken();
                    }
                });
    }

    private void checkSubscription(){
        if(token != null) {
            categoryRef.child(currentUsersTokenPath).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshots) {
                    for (DataSnapshot dataSnapshot : dataSnapshots.getChildren()) {
                        if (token.equals(dataSnapshot.getValue(String.class))) {
                            subscribeButton.setText(getResources().getString(R.string.unsubscribe));
                            break;
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void createSearchListener() {
        searchListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchCategoryAndRecipeList.clear();

                if(s.length() < 2) {
                    if(level > 0) {
                        searchCategoryAndRecipeList.addAll(categoryAndRecipeList);
                        searchBarIsEmpty.setVisibility(View.GONE);
                        listOfCategories.setVisibility(View.VISIBLE);
                    }
                    else{
                        emptyOrNotFoundText.setText(R.string.world_search);
                        searchBarIsEmpty.setVisibility(View.VISIBLE);
                        listOfCategories.setVisibility(View.GONE);
                    }
                }
                else{
                    String search = s.toString().toLowerCase();
                    if (level == 0)
                        searchFirstLevel = search;
                    for (CategoryOrRecipe item : categoryAndRecipeList) {
                        if (item.getName().toLowerCase().contains(search)) {
                            searchCategoryAndRecipeList.add(item);
                            Log.d("checking something",""+searchCategoryAndRecipeList.size());
                        }
                    }
                    if(searchCategoryAndRecipeList.size() == 0) {
                        String notFound = getResources().getString(R.string.world_search_not_found)
                                + " " +s.toString();
                        emptyOrNotFoundText.setText(notFound);
                        listOfCategories.setVisibility(View.GONE);
                        searchBarIsEmpty.setVisibility(View.VISIBLE);
                    } else{
                        searchBarIsEmpty.setVisibility(View.GONE);
                        listOfCategories.setVisibility(View.VISIBLE);
                    }
                }

                categoryRecipeRecyclerAdapter = new CategoryRecipeAdapter(searchCategoryAndRecipeList);
                if(level>0)
                    worldListener.updateMarkersData(searchCategoryAndRecipeList);
                categoryRecipeRecyclerAdapter.setOnClick(WorldCategoryFragment.this);
                categoryRecipeRecyclerView.setAdapter(categoryRecipeRecyclerAdapter);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }

    private void createListenerRecycler() {
        recyclerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                categoryAndRecipeList = utils.loadCategory(dataSnapshot);
                searchCategoryAndRecipeList.clear();
                if(level > 0) {
                    searchCategoryAndRecipeList.addAll(categoryAndRecipeList);
                    worldListener.updateMarkersData(searchCategoryAndRecipeList);
                    categoryRecipeRecyclerAdapter = new CategoryRecipeAdapter(searchCategoryAndRecipeList);
                    categoryRecipeRecyclerAdapter.setOnClick(WorldCategoryFragment.this);
                    categoryRecipeRecyclerView.setAdapter(categoryRecipeRecyclerAdapter);
                }else
                    searchField.setText(searchFirstLevel);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.d("Load Categories", "Failed to read value.", error.toException());
            }
        };
    }

    @Override
    public void onItemClick(int position, View v) {
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
        if (searchCategoryAndRecipeList.get(position) instanceof Recipe) {
            worldListener.toEnableOpenRecipeButton(true);
            worldListener.moveToMarker(position);
            worldListener.setPosition(position);

            categoryRecipeRecyclerAdapter.setLastClickedPosition(position);
            categoryRecipeRecyclerAdapter.notifyDataSetChanged();
        }

        else if (level == 0) {
            currentCategoryIndex = position;
            String nextSubCategoryName = searchCategoryAndRecipeList.get(position).getName();
            String newTitle = getResources().getString(R.string.world_category) +" - " +nextSubCategoryName;
            worldTitle.setText(newTitle);

            enterCategory(currentInnerItemsPath + nextSubCategoryName + "/");
        }
    }

    @Override
    public void onItemLongClick(int position, View v) {
        onItemClick(position, v);
    }

    private void enterCategory(final String path) {
        categoryRef.child(currentInnerItemsPath).removeEventListener(recyclerListener);

        if (!path.equals(basePath)) {
            currentInnerItemsPath = path + InnerIds.IN_CATEGORY;
            currentUsersTokenPath = path + InnerIds.USERS_TOKENS;
            checkSubscription();
        }

        worldListener.setCurrentPath(currentInnerItemsPath);

        // Loading the private categeries and recipies of the user and show them
        categoryRef.child(currentInnerItemsPath).addValueEventListener(recyclerListener);

        searchField.getText().clear();

        if (!currentInnerItemsPath.equals(basePath)){
            level = 1;
            subscribeButton.setVisibility(View.VISIBLE);
            searchField.clearFocus();
            searchBarIsEmpty.setVisibility(View.GONE);
            listOfCategories.setVisibility(View.VISIBLE);
        }
        worldListener.setLevel(level);
    }

    protected void fragmentBackToBaseCategories() {
        level = 0;
        worldTitle.setText(R.string.world_category);
        currentInnerItemsPath = basePath;
        enterCategory(basePath);
        searchBarIsEmpty.setVisibility(View.GONE);
        subscribeButton.setVisibility(View.GONE);
        subscribeButton.setText(getResources().getString(R.string.subscribe));
    }

    protected void removeListenersFromFragment() {
        searchField.removeTextChangedListener(searchListener);
        categoryRef.child(currentInnerItemsPath).removeEventListener(recyclerListener);
    }

    public interface OnWorldCategoryActionListener {
        void updateMarkersData(List<CategoryOrRecipe> searchCategoryAndRecipeList);

        void moveToMarker(int position);

        void toEnableOpenRecipeButton(boolean toEnable);

        void setCurrentPath(String currentPath);

        void setLevel(int level);

        void setRecyclerView(RecyclerView recyclerView);

        void setPosition(int position);

        void setFragment(WorldCategoryFragment fragment);

    }
}

