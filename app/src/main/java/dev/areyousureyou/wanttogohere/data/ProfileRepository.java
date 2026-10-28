package dev.areyousureyou.wanttogohere.data;

import java.io.IOException;
import java.util.List;

public interface ProfileRepository {
    List<WardProfile> getWards();

    List<TargetProfile> getTargets();

    void saveWard(WardProfile ward) throws IOException;

    boolean deleteWard(String wardId) throws IOException;

    void saveTarget(TargetProfile target) throws IOException;

    void deleteTarget(String targetId) throws IOException;
}
