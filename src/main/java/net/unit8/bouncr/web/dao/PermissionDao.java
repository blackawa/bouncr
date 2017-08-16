package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Permission;
import net.unit8.bouncr.web.entity.PermissionWithRealm;
import org.seasar.doma.*;

import java.util.List;

/**
 * A data access object for permission entity.
 *
 * @author kawasima
 */
@Dao(config = DomaConfig.class)
public interface PermissionDao {
    @Select
    List<Permission> selectAll();

    @Select
    List<PermissionWithRealm> selectByUserId(Long userId);

    @Insert
    int insert(Permission permission);

    @Update
    int update(Permission permission);

    @Delete
    int delete(Permission permission);
}
