package userservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import userservice.entity.Role;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {
}
