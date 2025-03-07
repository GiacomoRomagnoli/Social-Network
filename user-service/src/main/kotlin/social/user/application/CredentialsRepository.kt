package social.user.application

import social.common.ddd.Repository
import social.user.domain.Credentials
import social.user.domain.User.UserID

/**
 Repository to manage user credentials.
 */
interface CredentialsRepository : Repository<UserID, Credentials>
