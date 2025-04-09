package social.user.application

import social.common.ddd.Repository
import social.user.domain.User
import social.user.domain.UserID

/**
 * Repository to manage users.
 */
interface UserRepository : Repository<UserID, User>
