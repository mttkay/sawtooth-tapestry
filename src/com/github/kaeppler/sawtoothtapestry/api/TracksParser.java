package com.github.kaeppler.sawtoothtapestry.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.kaeppler.sawtoothtapestry.model.Track;

public class TracksParser {

    public List<Track> parse(InputStream istream) throws IOException, JSONException {
        String jsonString = readJsonString(istream);
        JSONArray tracksArray = new JSONArray(jsonString);
        ArrayList<Track> tracks = new ArrayList<Track>(tracksArray.length());
        for (int i = 0; i < tracksArray.length(); i++) {
            JSONObject trackObject = tracksArray.getJSONObject(i);
            tracks.add(parseTrack(trackObject));
        }
        return tracks;
    }

    private Track parseTrack(JSONObject trackObject) throws JSONException {
        Track track = new Track();

        track.setWaveformUrl(trackObject.getString("waveform_url"));

        return track;
    }

    private String readJsonString(InputStream istream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            sb.append(line);
            line = reader.readLine();
        }

        String jsonString = sb.toString();
        return jsonString;
    }

}
