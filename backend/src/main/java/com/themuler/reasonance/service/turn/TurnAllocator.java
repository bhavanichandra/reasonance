package com.themuler.reasonance.service.turn;

import com.themuler.reasonance.entity.Space;
import com.themuler.reasonance.exception.ResourceNotFoundException;
import com.themuler.reasonance.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TurnAllocator {

    private final SpaceRepository spaceRepository;

    @Transactional
    public Space allocateNextTurn(UUID spaceId) {
        Space space = spaceRepository.findByIdWithLock(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + spaceId));

        int current = space.getCurrentTurn();
        int nextTurn = current + 1;
        space.setCurrentTurn(nextTurn);

        return space;
    }
}
