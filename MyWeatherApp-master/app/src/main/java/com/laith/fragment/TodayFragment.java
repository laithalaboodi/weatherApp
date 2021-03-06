package com.laith.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.laith.weatherforecast.LocationActivity;
import com.laith.model.Weather;
import com.laith.weatherforecast.R;
import com.laith.preference.LocationPreference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TodayFragment extends Fragment {
    private CardView cardLocation;

    private TextView txtCity;
    private TextView txtTemperature, txtDescription, txtWind, txtPressure, txtHumidity, txtSunrise, txtSunset;
    private TextView txtIcon, txtUpdate;

    private TextView txtHourTmr;
    private TextView txtDescriptionTmr, txtWindTmr, txtPressureTmr, txtHumidityTmr;
    private TextView txtTemperatureTmr, txtIconTmr;

    private Weather weatherToday, weatherTomorrow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Get city
        String city = new LocationPreference(getActivity()).getCity();

        new FetchCurrentWeatherTask(getActivity(), city).execute();
        new FetchTommorrowWeatherTask(getActivity(), city).execute();

        View rootView = inflater.inflate(R.layout.fragment_current, container, false);

        //Location data
        cardLocation = (CardView) rootView.findViewById(R.id.cardLocation);
        cardLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), LocationActivity.class);
                startActivityForResult(intent, 100);
            }
        });
        txtCity = (TextView) rootView.findViewById(R.id.txtCity);

        //Current data
        txtTemperature = (TextView) rootView.findViewById(R.id.currentTemperature);
        txtDescription = (TextView) rootView.findViewById(R.id.currentDescription);
        txtWind = (TextView) rootView.findViewById(R.id.currentWind);
        txtPressure = (TextView) rootView.findViewById(R.id.currentPressure);
        txtHumidity = (TextView) rootView.findViewById(R.id.currentHumidity);
        txtSunrise = (TextView) rootView.findViewById(R.id.currentSunrise);
        txtSunset = (TextView) rootView.findViewById(R.id.currentSunset);

        txtIcon = (TextView) rootView.findViewById(R.id.weatherIcon);
        txtUpdate = (TextView) rootView.findViewById(R.id.txtUpdate);
        txtIcon.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/weather.ttf"));

        //Tomorrow data
        View layoutTmr = rootView.findViewById(R.id.layoutTomorrow);
        txtHourTmr = (TextView) layoutTmr.findViewById(R.id.txtHour);
        txtDescriptionTmr = (TextView) layoutTmr.findViewById(R.id.txtDescription);
        txtWindTmr = (TextView) layoutTmr.findViewById(R.id.txtWind);
        txtPressureTmr = (TextView) layoutTmr.findViewById(R.id.txtPressure);
        txtHumidityTmr = (TextView) layoutTmr.findViewById(R.id.txtHumidity);

        txtTemperatureTmr = (TextView) layoutTmr.findViewById(R.id.txtTemperature);
        txtIconTmr = (TextView) layoutTmr.findViewById(R.id.txtIcon);
        txtIconTmr.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/weather.ttf"));

        return rootView;
    }

    private void renderWeatherData() {
        txtCity.setText(weatherToday.getCityName() + ", " + weatherToday.getCountryName());
        txtTemperature.setText(weatherToday.getTemperature() + " ???");
        txtPressure.setText(getString(R.string.weather_pressure) + ": " + weatherToday.getPressure() + " hPa");
        txtHumidity.setText(getString(R.string.weather_humidity) + ": " + weatherToday.getHumidity() + " %");
        txtDescription.setText(weatherToday.getDescription());
        txtWind.setText(getString(R.string.weather_wind) + ": " + weatherToday.getWind() + " m/s");

        SimpleDateFormat df = new SimpleDateFormat("hh:mm a", Locale.US);
        txtSunrise.setText(getString(R.string.weather_sunrise) + ": " + df.format(weatherToday.getSunrise()));
        txtSunset.setText(getString(R.string.weather_sunset) + ": " + df.format(weatherToday.getSunset()));

        txtIcon.setText(getString(weatherToday.getWeatherIconId()));
        txtUpdate.setText(getString(R.string.weather_update) + ": " + df.format(weatherToday.getUpdate()));
    }

    private void renderWeatherDataTmr() {
        SimpleDateFormat df = new SimpleDateFormat("(MMMM dd, yyyy)", Locale.US);
        txtHourTmr.setText(getActivity().getString(R.string.weather_tmr) + " " + df.format(weatherTomorrow.getDate()));
        txtTemperatureTmr.setText(weatherTomorrow.getTemperature() + " ???");
        txtPressureTmr.setText(getActivity().getString(R.string.weather_pressure) + ": " + weatherTomorrow.getPressure() + " hPa");
        txtHumidityTmr.setText(getActivity().getString(R.string.weather_humidity) + ": " + weatherTomorrow.getHumidity() + " %");
        txtDescriptionTmr.setText(weatherTomorrow.getDescription());
        txtWindTmr.setText(getActivity().getString(R.string.weather_wind) + ": " + weatherTomorrow.getWind() + " m/s");
        txtIconTmr.setText(getActivity().getString(weatherTomorrow.getWeatherIconId()));
    }

    private class FetchCurrentWeatherTask extends AsyncTask<Void, Void, JSONObject> {
        private String OPEN_WEATHER_MAP_API =
                "http://api.openweathermap.org/data/2.5/weather?q=%s&units=metric";
        private ProgressDialog progressDialog;
        private Activity activity;
        private String city;

        public FetchCurrentWeatherTask(Activity activity, String city) {
            this.activity = activity;
            this.city = city;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(activity);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                URL url = new URL(String.format(OPEN_WEATHER_MAP_API, city));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("x-api-key", activity.getString(R.string.open_weather_maps_app_id));

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuffer json = new StringBuffer(1024);
                String tmp;
                while ((tmp = reader.readLine()) != null)
                    json.append(tmp).append("\n");
                reader.close();

                JSONObject data = new JSONObject(json.toString());

                if (data.getInt("cod") != 200) {
                    return null;
                }

                return data;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            progressDialog.dismiss();

            if (jsonObject == null)
                Toast.makeText(activity, activity.getString(R.string.error_location), Toast.LENGTH_LONG).show();
            else {
                weatherToday = new Weather();
                weatherToday.parseFromCurrent(jsonObject);
                renderWeatherData();
            }
        }
    }

    private class FetchTommorrowWeatherTask extends AsyncTask<Void, Void, JSONArray> {
        private String OPEN_WEATHER_MAP_API =
                "http://api.openweathermap.org/data/2.5/forecast?q=%s&units=metric";
        private ProgressDialog progressDialog;
        private Activity activity;
        private String city;

        public FetchTommorrowWeatherTask(Activity activity, String city) {
            this.activity = activity;
            this.city = city;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(activity);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected JSONArray doInBackground(Void... params) {
            try {
                URL url = new URL(String.format(OPEN_WEATHER_MAP_API, city));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("x-api-key", activity.getString(R.string.open_weather_maps_app_id));

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuffer json = new StringBuffer(1024);
                String tmp;
                while ((tmp = reader.readLine()) != null)
                    json.append(tmp).append("\n");
                reader.close();

                JSONObject data = new JSONObject(json.toString());

                if (data.getInt("cod") != 200) {
                    return null;
                }

                return data.getJSONArray("list");
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            super.onPostExecute(jsonArray);
            progressDialog.dismiss();

            try {
                if (jsonArray == null)
                    Toast.makeText(activity, activity.getString(R.string.error_location), Toast.LENGTH_LONG).show();
                else {
                    Calendar today = Calendar.getInstance();
                    Calendar curDay = Calendar.getInstance();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        curDay.setTimeInMillis(Long.parseLong(jsonObject.getString("dt") + "000"));

                        if (today.get(Calendar.DAY_OF_YEAR) < curDay.get(Calendar.DAY_OF_YEAR)) {
                            weatherTomorrow = new Weather();
                            weatherTomorrow.parseFromHourly(jsonArray.getJSONObject(i));
                            break;
                        }
                    }
                    renderWeatherDataTmr();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == 1) {
            String city = new LocationPreference(getActivity()).getCity();

            new FetchCurrentWeatherTask(getActivity(), city).execute();
            new FetchTommorrowWeatherTask(getActivity(), city).execute();
        }
    }
}

