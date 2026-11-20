package com.cinx.learning.wardrobe.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.engagement.application.IStreakService;
import com.cinx.learning.wardrobe.application.command.EquipOwlSkinsCommand;
import com.cinx.learning.wardrobe.application.command.UpdateOwlSkinCommand;
import com.cinx.learning.wardrobe.application.query.OwlSkinView;
import com.cinx.learning.wardrobe.application.query.OwlWardrobeView;
import com.cinx.learning.wardrobe.domain.OwlSkin;
import com.cinx.learning.wardrobe.domain.OwlSkinGrant;
import com.cinx.learning.wardrobe.domain.OwlSkinGrantRepository;
import com.cinx.learning.wardrobe.domain.OwlSkinRepository;
import com.cinx.learning.wardrobe.domain.OwlSkinSlot;
import com.cinx.learning.wardrobe.domain.OwlSkinUnlockRule;
import com.cinx.learning.wardrobe.domain.OwlWardrobe;
import com.cinx.learning.wardrobe.domain.OwlWardrobeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwlWardrobeService implements IOwlWardrobeService {
    /** Seasons follow the Vietnamese calendar day, as the daily quiz does. */
    static final ZoneId SEASON_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OwlSkinRepository skinRepository;
    private final OwlWardrobeRepository wardrobeRepository;
    private final OwlSkinGrantRepository grantRepository;
    private final IStreakService streakService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public OwlWardrobeView getWardrobe(String userId) {
        return view(userId);
    }

    @Override
    @Transactional
    public OwlWardrobeView equip(String userId, EquipOwlSkinsCommand command) {
        Owned owned = owned(userId);
        OwlWardrobe wardrobe = wardrobeRepository.findByUserId(userId)
                .orElseGet(() -> OwlWardrobe.builder().userId(userId).build());
        wardrobe.setPalette(checked(command.palette(), OwlSkinSlot.PALETTE, owned));
        wardrobe.setHead(checked(command.head(), OwlSkinSlot.HEAD, owned));
        wardrobe.setFace(checked(command.face(), OwlSkinSlot.FACE, owned));
        wardrobe.setEffect(checked(command.effect(), OwlSkinSlot.EFFECT, owned));
        wardrobe.setHold(checked(command.hold(), OwlSkinSlot.HOLD, owned));
        wardrobeRepository.save(wardrobe);
        return view(userId);
    }

    @Override
    @Transactional
    public void grantOpenSeasonSkins(String userId) {
        LocalDate today = LocalDate.now(clock.withZone(SEASON_ZONE));
        List<String> granted = grantRepository.findByUserId(userId).stream().map(OwlSkinGrant::getSkinCode).toList();
        for (OwlSkin skin : skinRepository.findAllByOrderBySortOrderAsc()) {
            if (Boolean.TRUE.equals(skin.getActive()) && skin.isSeasonOpenOn(today) && !granted.contains(skin.getCode())) {
                grantRepository.save(OwlSkinGrant.builder()
                        .userId(userId)
                        .skinCode(skin.getCode())
                        .grantedAt(LocalDateTime.now(clock.withZone(ZoneOffset.UTC)))
                        .build());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwlSkinView> getCatalog() {
        return skinRepository.findAllByOrderBySortOrderAsc().stream().map(OwlWardrobeService::toView).toList();
    }

    @Override
    @Transactional
    public OwlSkinView updateSkin(String code, UpdateOwlSkinCommand command) {
        OwlSkin skin = skinRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Owl skin not found with code: " + code));
        if (command.active() != null) {
            skin.setActive(command.active());
        }
        if (command.seasonStartsOn() != null || command.seasonEndsOn() != null) {
            if (skin.getUnlockRule() != OwlSkinUnlockRule.SEASON) {
                throw new BadRequestException(ErrorCode.VALIDATION_FAILED, "Only season skins have dates");
            }
            LocalDate starts = command.seasonStartsOn() != null ? command.seasonStartsOn() : skin.getSeasonStartsOn();
            LocalDate ends = command.seasonEndsOn() != null ? command.seasonEndsOn() : skin.getSeasonEndsOn();
            if (starts == null || ends == null || ends.isBefore(starts)) {
                throw new BadRequestException(ErrorCode.VALIDATION_FAILED, "A season needs a start on or before its end");
            }
            skin.setSeasonStartsOn(starts);
            skin.setSeasonEndsOn(ends);
        }
        return toView(skinRepository.save(skin));
    }

    /** The learner's catalog with what is theirs: free skins, streak skins their best streak reached, earned seasons. */
    private record Owned(List<OwlSkin> catalog, int highestStreak, Map<String, LocalDateTime> grants) {
        boolean has(OwlSkin skin) {
            return switch (skin.getUnlockRule()) {
                case FREE -> true;
                case STREAK -> skin.getStreakDays() != null && highestStreak >= skin.getStreakDays();
                case SEASON -> grants.containsKey(skin.getCode());
            };
        }

        Optional<OwlSkin> active(String code) {
            return catalog.stream().filter(skin -> skin.getCode().equals(code) && Boolean.TRUE.equals(skin.getActive())).findFirst();
        }
    }

    private Owned owned(String userId) {
        Integer highest = streakService.getUserStreak(userId).getHighestStreak();
        Map<String, LocalDateTime> grants = grantRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(OwlSkinGrant::getSkinCode, OwlSkinGrant::getGrantedAt, (a, b) -> a));
        return new Owned(skinRepository.findAllByOrderBySortOrderAsc(), highest == null ? 0 : highest, grants);
    }

    private static String checked(String code, OwlSkinSlot slot, Owned owned) {
        if (code == null || code.isBlank()) {
            return null;
        }
        OwlSkin skin = owned.active(code)
                .orElseThrow(() -> new BadRequestException(ErrorCode.VALIDATION_FAILED, "Unknown or inactive owl skin: " + code));
        if (skin.getSlot() != slot) {
            throw new BadRequestException(ErrorCode.VALIDATION_FAILED, "Owl skin " + code + " is not worn as " + slot);
        }
        if (!owned.has(skin)) {
            throw new BadRequestException(ErrorCode.VALIDATION_FAILED, "Owl skin " + code + " is still locked");
        }
        return code;
    }

    private OwlWardrobeView view(String userId) {
        Owned owned = owned(userId);
        LocalDate today = LocalDate.now(clock.withZone(SEASON_ZONE));
        OwlWardrobe wardrobe = wardrobeRepository.findByUserId(userId).orElse(null);
        List<OwlWardrobeView.Item> items = owned.catalog().stream()
                .filter(skin -> Boolean.TRUE.equals(skin.getActive()))
                .map(skin -> new OwlWardrobeView.Item(skin.getCode(), skin.getSlot(), skin.getUnlockRule(), skin.getStreakDays(),
                        skin.getSeasonStartsOn(), skin.getSeasonEndsOn(), skin.isSeasonOpenOn(today), owned.has(skin),
                        owned.grants().get(skin.getCode())))
                .toList();
        // an item an admin switched off is no longer worn
        OwlWardrobeView.Equipped equipped = wardrobe == null
                ? new OwlWardrobeView.Equipped(null, null, null, null, null)
                : new OwlWardrobeView.Equipped(worn(wardrobe.getPalette(), owned), worn(wardrobe.getHead(), owned),
                        worn(wardrobe.getFace(), owned), worn(wardrobe.getEffect(), owned), worn(wardrobe.getHold(), owned));
        return new OwlWardrobeView(equipped, items, owned.highestStreak());
    }

    private static String worn(String code, Owned owned) {
        return code != null && owned.active(code).isPresent() ? code : null;
    }

    private static OwlSkinView toView(OwlSkin skin) {
        return new OwlSkinView(skin.getCode(), skin.getSlot(), skin.getUnlockRule(), skin.getStreakDays(),
                skin.getSeasonStartsOn(), skin.getSeasonEndsOn(), skin.getName(), skin.getActive(), skin.getSortOrder());
    }
}
