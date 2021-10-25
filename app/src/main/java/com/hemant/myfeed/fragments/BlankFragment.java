package com.hemant.myfeed.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cooltechworks.views.shimmer.ShimmerRecyclerView;
import com.einmalfel.earl.EarlParser;
import com.einmalfel.earl.Enclosure;
import com.einmalfel.earl.Feed;
import com.einmalfel.earl.Item;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.hemant.myfeed.R;
import com.hemant.myfeed.Util.CustomItemClickListener;
import com.hemant.myfeed.Util.RVAdapter;
import com.prof.rssparser.Article;
import com.prof.rssparser.Parser;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class BlankFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";

    @BindView(R.id.rv)
    ShimmerRecyclerView rv;
    @BindView(R.id.adView)
    AdView adView;
    private ArrayList<Item> RssItems;
    private String mParam1;


    private OnFragmentInteractionListener mListener;

    public BlankFragment() {
    }


    public static BlankFragment newInstance(String param1) {
        BlankFragment fragment = new BlankFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_blank, container, false);
        ButterKnife.bind(this, rootView);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(llm);
        rv.setHasFixedSize(false);
        rv.showShimmerAdapter();
        RssItems = new ArrayList<>();
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        new GetRssFeed().execute(mParam1);
        return rootView;
    }
    private void secondaryParser(){
        Parser parser = new Parser();
        parser.execute(mParam1);
        parser.onFinish(new Parser.OnTaskCompleted() {

            @Override
            public void onTaskCompleted(ArrayList<Article> list) {

                for (final Article article : list){
                    Item item = new Item() {
                        @Nullable
                        @Override
                        public String getLink() {
                            return article.getLink();
                        }

                        @Nullable
                        @Override
                        public Date getPublicationDate() {
                            return article.getPubDate();
                        }

                        @Nullable
                        @Override
                        public String getTitle() {
                            return article.getTitle();
                        }

                        @Nullable
                        @Override
                        public String getDescription() {
                            return article.getDescription();
                        }

                        @Nullable
                        @Override
                        public String getImageLink() {
                            return article.getImage();
                        }

                        @Nullable
                        @Override
                        public String getAuthor() {
                            return article.getAuthor();
                        }

                        @NonNull
                        @Override
                        public List<? extends Enclosure> getEnclosures() {
                            return null;
                        }
                    };
                    RssItems.add(item);
                }
                initializeAdapter();
            }

            @Override
            public void onError() {
                Toast.makeText(getContext(),"Cannot get NEWS Feed. Try any other channel.",Toast.LENGTH_LONG).show();
            }
        });
    }
    private void initializeAdapter() {

        RVAdapter adapter = new RVAdapter(getActivity(), RssItems, new CustomItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                String url = RssItems.get(position).getLink();
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(getActivity(), Uri.parse(url));
            }
        });
        adapter.notifyDataSetChanged();
        if (rv != null) {
            rv.hideShimmerAdapter();
            rv.setAdapter(adapter);
    }

    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
    private class GetRssFeed extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                InputStream inputStream = new URL(params[0]).openConnection().getInputStream();
                Feed feed = EarlParser.parseOrThrow(inputStream, 0);

                RssItems.addAll(feed.getItems());

            } catch (Exception e) {
                Log.v("Error Parsing Data", e + "");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (RssItems.isEmpty()){
                Toast.makeText(getContext(), "Try other approach.", Toast.LENGTH_SHORT).show();
                secondaryParser();
            }
            else {
                initializeAdapter();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        }

}
