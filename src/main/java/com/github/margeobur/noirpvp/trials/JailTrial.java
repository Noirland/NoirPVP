package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.PVPPlayer;

import java.time.LocalDateTime;

public class JailTrial extends Trial {

    public JailTrial(PVPPlayer attacker) {
        super(attacker);
    }

    public JailTrial(PVPPlayer attacker, LocalDateTime initTime) {
        super(attacker, initTime);
    }


}
