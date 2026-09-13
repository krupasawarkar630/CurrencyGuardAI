package com.example.currencyguard.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.currencyguard.R;
import com.example.currencyguard.activity.ChatAssistantActivity;
import com.example.currencyguard.adapter.LearnContentAdapter;
import com.example.currencyguard.model.LearnTopic;

import java.util.ArrayList;
import java.util.List;

public class LearnFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_learn, container, false);

        view.findViewById(R.id.btn_open_chat).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ChatAssistantActivity.class));
        });

        RecyclerView rvTopics = view.findViewById(R.id.rv_learn_topics);
        rvTopics.setLayoutManager(new LinearLayoutManager(getContext()));

        LearnContentAdapter adapter = new LearnContentAdapter();
        adapter.setTopics(createEducationalTopics());
        rvTopics.setAdapter(adapter);

        return view;
    }

    private List<LearnTopic> createEducationalTopics() {
        List<LearnTopic> list = new ArrayList<>();

        list.add(new LearnTopic(
                "wm",
                "1. Watermark Window",
                "Substrate Feature",
                "Embedded during the paper manufacturing process by varying fiber density. Displays the Mahatma Gandhi portrait alongside multi-directional electrotype denomination markings.",
                "🔍 How to check: Hold note against light. Genuine watermarks are seamlessly graduated with light and dark tones, never flat surface printing."
        ));

        list.add(new LearnTopic(
                "st",
                "2. Security Thread",
                "Embedded Feature",
                "A specialized metallic/polymer ribbon woven into the paper. On the front, it appears windowed; when held against transmitted light, it reveals a continuous unbroken strip inscribed with 'RBI' and the denomination.",
                "🔍 How to check: Tilt note to observe the color shift from green to blue under varying light angles."
        ));

        list.add(new LearnTopic(
                "mt",
                "3. Microprinting & Microtext",
                "Fine Engraving",
                "Extremely small typographic characters printed across the portrait border and typography (e.g., 'RBI' and '500') that appear as solid lines to the naked eye.",
                "🔍 How to check: Use a magnifying glass (5x-10x). Genuine microprinting is sharp, distinct, and legible without ink bleed."
        ));

        list.add(new LearnTopic(
                "ip",
                "4. Raised Intaglio Printing",
                "Tactile Characteristic",
                "Heavy ink transferred under immense hydraulic pressure, creating raised relief on the Mahatma Gandhi portrait, Reserve Bank seal, and bleeding identity marks.",
                "🔍 How to check: Gently rub your fingertip over the portrait and Ashoka Pillar emblem to feel the distinct raised texture."
        ));

        list.add(new LearnTopic(
                "st_reg",
                "5. See-Through Registration Mark",
                "Precision Alignment",
                "A segmented numeral printed partly on the front and partly on the back in exact corresponding coordinates. When held against light, the segments combine into a complete numeral.",
                "🔍 How to check: Hold note against bright light. The back and front shapes must align with micrometer precision."
        ));

        list.add(new LearnTopic(
                "cs_ink",
                "6. Color-Shifting Optically Variable Ink",
                "Optical Security",
                "The large numeral marking on high-denomination notes is printed in specialized pigment that dynamically changes hue from green to blue upon tilting.",
                "🔍 How to check: Tilt the note horizontally and vertically. Genuine pigment shifts color cleanly without glitter or peeling."
        ));

        list.add(new LearnTopic(
                "sn",
                "7. Number Panels & Serial Sequence",
                "Serial Security",
                "Serial numbers printed with specialized magnetic and fluorescent inks in ascending font sizes from left to right.",
                "🔍 How to check: Verify character geometry and check under UV illumination for vivid red/green phosphorescent glow."
        ));

        list.add(new LearnTopic(
                "dim",
                "8. Dimensional Standards",
                "Physical Geometry",
                "Each denomination conforms to strict dimensions set by the Central Bank (e.g. ₹500 is exactly 66 mm x 150 mm).",
                "🔍 How to check: Measure note dimensions. Counterfeits often exhibit incorrect aspect ratios or irregular cut margins."
        ));

        return list;
    }
}
